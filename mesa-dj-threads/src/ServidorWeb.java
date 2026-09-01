import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * ServidorWeb — a "frente" web da aplicação.
 *
 * Usa só métodos que já existiam em MesaDJ/Musica (tocar, pausar,
 * status, alternarPausaTudo, alternarMuteTudo, ajustarBpmGlobal,
 * definirBpmGlobal, setMusicaAtual, sair). NADA nesta classe exige
 * qualquer alteração em Faixa.java, Musica.java ou MesaDJ.java —
 * eles continuam exatamente como estão.
 *
 * Como rodar (a partir da pasta src/, igual ao App.java):
 *   javac *.java
 *   java ServidorWeb
 *   abrir http://localhost:8080 no navegador
 */
public class ServidorWeb {

    private final MesaDJ mesa;
    private final Map<String, Musica> catalogo;
    private volatile Musica musicaAtual;
    private final int porta;

    public ServidorWeb(MesaDJ mesa, Musica musicaInicial, Map<String, Musica> catalogo, int porta) {
        this.mesa = mesa;
        this.musicaAtual = musicaInicial;
        this.catalogo = catalogo;
        this.porta = porta;
    }

    public void iniciar() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(porta), 0);

        server.createContext("/", this::servirArquivoEstatico);
        server.createContext("/api/status", this::handleStatus);
        server.createContext("/api/musicas", this::handleMusicas);
        server.createContext("/api/tocar", this::handleTocar);
        server.createContext("/api/pausar", this::handlePausar);
        server.createContext("/api/musica", this::handleTrocarMusica);
        server.createContext("/api/bpm", this::handleBpm);
        server.createContext("/api/pausar-tudo", this::handlePausarTudo);
        server.createContext("/api/mute", this::handleMute);
        server.createContext("/api/sair", this::handleSair);

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
    }

    // =========================================================
    // Arquivos estáticos (web/index.html, style.css, script.js)
    // =========================================================

    private void servirArquivoEstatico(HttpExchange exchange) throws IOException {
        String caminho = exchange.getRequestURI().getPath();
        if (caminho.equals("/")) {
            caminho = "/index.html";
        }

        File arquivo = new File("../web" + caminho);

        if (!arquivo.exists() || arquivo.isDirectory()) {
            responderTexto(exchange, 404, "Arquivo não encontrado: " + caminho);
            return;
        }

        byte[] bytes = Files.readAllBytes(arquivo.toPath());
        exchange.getResponseHeaders().add("Content-Type", tipoConteudo(caminho));
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream saida = exchange.getResponseBody()) {
            saida.write(bytes);
        }
    }

    private String tipoConteudo(String caminho) {
        if (caminho.endsWith(".html")) return "text/html; charset=utf-8";
        if (caminho.endsWith(".css")) return "text/css; charset=utf-8";
        if (caminho.endsWith(".js")) return "application/javascript; charset=utf-8";
        return "application/octet-stream";
    }

    // =========================================================
    // Endpoints de leitura (GET)
    // =========================================================

    private void handleStatus(HttpExchange exchange) throws IOException {
        Map<Integer, String> status = mesa.status();
        Musica musicaLida = musicaAtual;

        Faixa qualquerFaixa = musicaLida.getFaixas().values().stream().findFirst().orElse(null);
        int bpmAtual = qualquerFaixa != null ? qualquerFaixa.getBpm() : 0;

        StringBuilder json = new StringBuilder();
        json.append("{\"musica\":\"").append(escaparJson(musicaLida.getNome())).append("\",");
        json.append("\"bpm\":").append(bpmAtual).append(",");
        json.append("\"faixas\":[");

        boolean primeiro = true;
        for (Map.Entry<Integer, String> entry : status.entrySet()) {
            if (!primeiro) json.append(",");
            primeiro = false;

            String[] partes = entry.getValue().split(": ", 2);
            String nome = partes[0];
            boolean tocando = partes.length > 1 && "TOCANDO".equals(partes[1]);

            json.append("{\"tecla\":").append(entry.getKey())
                .append(",\"nome\":\"").append(escaparJson(nome)).append("\"")
                .append(",\"tocando\":").append(tocando)
                .append("}");
        }
        json.append("]}");

        responderJson(exchange, json.toString());
    }

    private void handleMusicas(HttpExchange exchange) throws IOException {
        StringBuilder json = new StringBuilder("[");
        boolean primeiro = true;
        for (Map.Entry<String, Musica> entry : catalogo.entrySet()) {
            if (!primeiro) json.append(",");
            primeiro = false;
            json.append("{\"id\":\"").append(entry.getKey()).append("\",")
                .append("\"nome\":\"").append(escaparJson(entry.getValue().getNome())).append("\"}");
        }
        json.append("]");
        responderJson(exchange, json.toString());
    }

    // =========================================================
    // Endpoints de ação (POST)
    // =========================================================

    private void handleTocar(HttpExchange exchange) throws IOException {
        if (!exigirPost(exchange)) return;
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        int tecla = Integer.parseInt(params.getOrDefault("tecla", "-1"));
        mesa.tocar(tecla);
        responderTexto(exchange, 200, "ok");
    }

    private void handlePausar(HttpExchange exchange) throws IOException {
        if (!exigirPost(exchange)) return;
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        int tecla = Integer.parseInt(params.getOrDefault("tecla", "-1"));
        mesa.pausar(tecla);
        responderTexto(exchange, 200, "ok");
    }

    private synchronized void handleTrocarMusica(HttpExchange exchange) throws IOException {
        if (!exigirPost(exchange)) return;
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        String id = params.get("id");
        Musica nova = catalogo.get(id);

        if (nova == null) {
            responderTexto(exchange, 404, "Música não encontrada: " + id);
            return;
        }

        musicaAtual.pararFaixas();
        nova.carregarFaixas();
        musicaAtual = nova;
        mesa.setMusicaAtual(musicaAtual);

        responderTexto(exchange, 200, "ok");
    }

    private void handleBpm(HttpExchange exchange) throws IOException {
        if (!exigirPost(exchange)) return;
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());

        if (params.containsKey("delta")) {
            mesa.ajustarBpmGlobal(Integer.parseInt(params.get("delta")));
        } else if (params.containsKey("valor")) {
            mesa.definirBpmGlobal(Integer.parseInt(params.get("valor")));
        }

        responderTexto(exchange, 200, "ok");
    }

    private void handlePausarTudo(HttpExchange exchange) throws IOException {
        if (!exigirPost(exchange)) return;
        mesa.alternarPausaTudo();
        responderTexto(exchange, 200, "ok");
    }

    private void handleMute(HttpExchange exchange) throws IOException {
        if (!exigirPost(exchange)) return;
        mesa.alternarMuteTudo();
        responderTexto(exchange, 200, "ok");
    }

    private void handleSair(HttpExchange exchange) throws IOException {
        if (!exigirPost(exchange)) return;
        mesa.sair();
        responderTexto(exchange, 200, "ok");
    }

    // =========================================================
    // Auxiliares
    // =========================================================

    private boolean exigirPost(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            responderTexto(exchange, 405, "Método não permitido, use POST.");
            return false;
        }
        return true;
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;

        for (String par : query.split("&")) {
            String[] kv = par.split("=", 2);
            String chave = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String valor = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            params.put(chave, valor);
        }
        return params;
    }

    private String escaparJson(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void responderTexto(HttpExchange exchange, int status, String corpo) throws IOException {
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream saida = exchange.getResponseBody()) {
            saida.write(bytes);
        }
    }

    private void responderJson(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream saida = exchange.getResponseBody()) {
            saida.write(bytes);
        }
    }

    // =========================================================
    // Entrada da aplicação (independente do App.java de console)
    // =========================================================

    public static void main(String[] args) throws IOException {
        Map<String, Musica> catalogo = new HashMap<>();
        catalogo.put("1", new Musica(1, "Finesse - Bruno Mars", "musica1"));
        catalogo.put("2", new Musica(2, "Dark Horse - Katy Perry", "musica2"));
        catalogo.put("3", new Musica(3, "Bad Guy - Billie Eilish", "musica3"));

        Musica musicaInicial = catalogo.get("1");
        musicaInicial.carregarFaixas();

        MesaDJ mesa = new MesaDJ(musicaInicial);

        ServidorWeb servidor = new ServidorWeb(mesa, musicaInicial, catalogo, 8080);
        servidor.iniciar();

        System.out.println("Mesa DJ rodando em http://localhost:8080");
    }
}
