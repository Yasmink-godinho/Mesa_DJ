import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("  INICIANDO MESA DJ - MIXER CONCORRENTE ");
        System.out.println("=========================================");

        // Catálogo de músicas disponíveis
        Map<String, Musica> catalogo = new HashMap<>();
        catalogo.put("1", new Musica(1, "Finesse - Bruno Mars", "musica1"));
        catalogo.put("2", new Musica(2, "Música 2 (Stems)", "musica2"));
        catalogo.put("3", new Musica(3, "Música 3 (Stems)", "musica3"));

        // Inicializa com a Música 1
        Musica musicaAtual = catalogo.get("1");
        System.out.println("Carregando música inicial: " + musicaAtual.getNome() + "...");
        musicaAtual.carregarFaixas();

        MesaDJ mesa = new MesaDJ(musicaAtual);

        // Aguarda carregar o áudio na memória
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Scanner scanner = new Scanner(System.in);
        boolean executando = true;

        exibirInstrucoes(musicaAtual);

        
        while (executando) {
            System.out.print("\nComando > ");
            String entrada = scanner.nextLine().trim().toLowerCase();

            if (entrada.equals("bpm+")) {
            mesa.ajustarBpmGlobal(10);
            continue;
             }

            
             if (entrada.equals("bpm-")) {
            mesa.ajustarBpmGlobal(-10);
            continue;
             }
            
             if (entrada.startsWith("bpm ")) {
            try {
                int novoBpm = Integer.parseInt(entrada.substring(4));
                mesa.definirBpmGlobal(novoBpm);
            } catch (NumberFormatException e) {
                System.out.println("X BPM inválido. Digite, por exemplo: bpm 140");
            }
            continue;
          }
            // 1. Tocar faixa individual: apenas números (ex: "1", "2", "12", "18")
            if (entrada.matches("\\d+")) {
                int numeroFaixa = Integer.parseInt(entrada);
                mesa.tocar(numeroFaixa);

                // 2. Pausar faixa individual: 'p' seguido de número (ex: "p1", "p2", "p12")
            } else if (entrada.startsWith("p") && entrada.substring(1).matches("\\d+")) {
                int numeroFaixa = Integer.parseInt(entrada.substring(1));
                mesa.pausar(numeroFaixa);

                // 3. Trocar de Música: 'm' seguido de número (ex: "m1", "m2", "m3", "m4")
            } else if (entrada.startsWith("m") && entrada.substring(1).matches("\\d+")) {
                String idMusica = entrada.substring(1);

                if (catalogo.containsKey(idMusica)) {
                    System.out.println("\nTrocando para: " + catalogo.get(idMusica).getNome() + "...");

                    // 1. Para a música atual e libera os recursos
                    musicaAtual.pararFaixas();

                    // 2. Atualiza a referência e carrega as faixas da nova música
                    musicaAtual = catalogo.get(idMusica);
                    musicaAtual.carregarFaixas();
                    mesa.setMusicaAtual(musicaAtual);

                    System.out.println("Música carregada com sucesso!");
                    exibirInstrucoes(musicaAtual);
                } else {
                    System.out.println("Musica '" + idMusica + "' nao encontrada no catalogo.");
                }

                // 4. Comandos globais e atalhos de texto
            } else {
                switch (entrada) {
                    case "t":
                        mesa.alternarPausaTudo();
                        break;
                    case "mute": // ou pode usar outro atalho para mute global
                        mesa.alternarMuteTudo();
                        break;
                    case "s":
                        System.out.println("\n--- STATUS DAS FAIXAS (" + musicaAtual.getNome() + ") ---");
                        mesa.status().forEach((tecla, info) -> System.out.println(" [Canal " + tecla + "] -> " + info));
                        break;
                    case "ajuda":
                        exibirInstrucoes(musicaAtual);
                        break;
                    case "sair":
                        mesa.sair();
                        executando = false;
                        break;
                    default:
                        System.out.println("Comando nao reconhecido. Digite 'ajuda' para ver os comandos.");
                        break;
                }
            }
        }

        scanner.close();
        System.out.println("Aplicação encerrada.");
    }

    private static void exibirInstrucoes(Musica musicaAtual) {
        System.out.println("\n-----------------------------------------");
        System.out.println("🎵 Música Atual: " + musicaAtual.getNome());
        System.out.println("-----------------------------------------");
        System.out.println(" • Digite (1 a 12)  -> TOCAR/RETOMAR faixa individual");
        System.out.println(" • Digite (p1 a p12) -> PAUSAR faixa individual");
        System.out.println(" • Digite 't'            -> Pausar/Continuar Tudo");
        System.out.println(" • Digite 'mute'            -> Master Play/Mute Geral");
        System.out.println(" • Digite 's'            -> Ver Status dos canais");
        System.out.println(" • Digite 'm1', 'm2', 'm3' -> Trocar de Música");
        System.out.println(" • Digite 'ajuda'       -> Mostrar instruçoes novamente");
        System.out.println(" • Digite 'sair'         -> Encerrar a Mesa");
        System.out.println("-----------------------------------------");
    }
}