import java.util.LinkedHashMap; // Estrutura de dados que mantém a ordem de inserção das faixas
import java.util.Map; // Interface para mapeamento de chave e valor

// Representa a entidade Música, agrupando e gerenciando seus 4 canais concorrentes (stems)
public class Musica {

    // Atributos privados da música
    private int id; // Identificador único da música (ex: 1, 2, 3)
    private String nome; // Nome da música (ex: "tcheca na tcheca")
    private String pasta; // Nome da subpasta dentro de audio/ onde ficam os .wav
    private Map<Integer, Faixa> faixas; // Dicionário que mapeia a tecla (1 a 12) para a sua respectiva Faixa

    // Construtor da classe Musica
    public Musica(int id, String nome, String pasta) {
        this.id = id; // Atribui o ID da música
        this.nome = nome; // Atribui o nome da música
        this.pasta = pasta; // Atribui o nome da pasta de arquivos
        faixas = new LinkedHashMap<>(); // Inicializa o mapa preservando a ordem das faixas (1 a 12)
    }

    // Retorna o ID da música
    public int getId() {
        return id;
    }

    // Retorna o nome da música
    public String getNome() {
        return nome;
    }

    // Retorna a coleção com as X faixas ativas
    public Map<Integer, Faixa> getFaixas() {
        return faixas;
    }

    // Carrega os X arquivos .wav da pasta na memória RAM e inicia as Threads de
    // cada canal
    public void carregarFaixas() {
        pararFaixas(); // Garante que qualquer áudio anterior seja interrompido e descarregado
        faixas.clear(); // Limpa referências antigas do mapa

        // Instancia cada uma das 4 faixas associando-a ao nome, caminho do .wav e
        // número da tecla
        faixas.put(1, new Faixa("Bateria", "audio/" + pasta + "/01 Kit Stem.wav", 1));
        faixas.put(2, new Faixa("Baixo", "audio/" + pasta + "/04 Bass Stem.wav", 2));
        faixas.put(3, new Faixa("Guitarra", "audio/" + pasta + "/08 Synth Stem.wav", 3));
        faixas.put(4, new Faixa("Violão", "audio/" + pasta + "/05 Ld Voc Stem.wav", 4));
        faixas.put(5, new Faixa("Voz Principal",   "audio/" + pasta + "/05 Ld Voc Stem.wav", 5));
        faixas.put(6, new Faixa("Adlibs",          "audio/" + pasta + "/06 Adlibs Stem.wav", 6));
        faixas.put(7, new Faixa("Backing Vocal",   "audio/" + pasta + "/07 BGV Stem.wav", 7));
        faixas.put(8, new Faixa("Sintetizador",    "audio/" + pasta + "/08 Synth Stem.wav", 8));
        faixas.put(9, new Faixa("Teclados",        "audio/" + pasta + "/09 Keys Stem.wav", 9));
        faixas.put(10, new Faixa("Coral",           "audio/" + pasta + "/10 Choir Stem.wav", 10));
        faixas.put(11, new Faixa("Hits",            "audio/" + pasta + "/11 Hits Stem.wav", 11));
        faixas.put(12, new Faixa("Efeitos (FX)",    "audio/" + pasta + "/12 FX Stem.wav", 12));

        // Cria e dispara uma Thread dedicada para cada faixa carregar seu Clip em
        // paralelo
        for (Faixa faixa : faixas.values()) {
            new Thread(faixa).start(); // Dispara a execução do método run() da Faixa
        }
    }

    // Desliga e libera a memória de todas as X faixas
    public void pararFaixas() {
        for (Faixa faixa : faixas.values()) {
            faixa.parar(); // Encerra a thread e fecha a linha de áudio de cada canal
        }
        faixas.clear(); // Remove os elementos da coleção
    }

    // synchronized: Alterna globalmente entre tocar todas as faixas juntas ou
    // silenciar todas
    public synchronized void alternarMaster() {
        // Verifica se ao menos uma faixa está emitindo som no momento
        boolean algumaTocando = faixas.values().stream().anyMatch(Faixa::isTocando);

        if (algumaTocando) {
            // Se houver faixas tocando, muta todas
            for (Faixa f : faixas.values()) {
                if (f.isTocando())
                    f.alternar();
            }
            System.out.println(" [MASTER MUTE] Todas as faixas foram silenciadas.");
        } else {
            // Se todas estiverem mudas, ativa todas sincronizadas do início
            for (Faixa f : faixas.values()) {
                if (!f.isTocando())
                    f.alternar();
            }
            System.out.println(" [MASTER PLAY] Todas as faixas tocando sincronizadas.");
        }
    }

    // synchronized: Pausa ou retoma todas as faixas mantendo a posição exata da
    // música
    public synchronized void alternarPausaGlobal() {
        // Checa se há canais emitindo áudio
        boolean algumaTocando = faixas.values().stream().anyMatch(Faixa::isTocando);

        if (algumaTocando) {
            // Congela todas as faixas no frame atual
            for (Faixa f : faixas.values())
                f.pausar();
            System.out.println("⏸ [MASTER PAUSE] Reprodução pausada.");
        } else {
            // Retoma todas as faixas a partir do ponto onde foram congeladas
            for (Faixa f : faixas.values())
                f.retomar();
            System.out.println("▶ [MASTER RESUME] Reprodução continuada do mesmo ponto.");
        }
    }

    // synchronized: Ajusta o BPM de todas as faixas da música simultaneamente
    public synchronized void ajustarBpmGlobal(int delta) {
        for (Faixa f : faixas.values()) {
            int novoBpm = Math.max(40, f.getBpm() + delta); // Impede que o BPM fique abaixo de 40
            f.setBpm(novoBpm); // Aplica a nova velocidade com sincronização
        }
    }
}