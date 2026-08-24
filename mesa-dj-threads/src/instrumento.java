import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;

public class Instrumento implements Runnable {

    // Atributos visuais e de controle
    private String nome;
    private String somTexto;
    private int intervaloMs;
    private boolean tocando;
    private boolean ativo;

    // Atributos de áudio MIDI nativo do Java
    private int canalNumero;        // Canal 9 = bateria/percussão; 0, 1, 2 = outros instrumentos
    private int instrumentoId;      // ID do timbre (0 = Piano, 33 = Baixo, 81 = Synth)
    private int notaMidi;           // Código da nota musical (ex: 36 = Bumbo, 48 = Dó)
    private MidiChannel canalMidi;  // Objeto do Java que emite a nota musical

    // Construtor recebendo os dados do som MIDI
    public Instrumento(String nome, String somTexto, int intervaloMs, int canalNumero, int instrumentoId, int notaMidi) {
        this.nome = nome;
        this.somTexto = somTexto;
        this.intervaloMs = intervaloMs;
        this.canalNumero = canalNumero;
        this.instrumentoId = instrumentoId;
        this.notaMidi = notaMidi;
        this.tocando = true;
        this.ativo = true;
    }

    public String getNome() {
        return nome;
    }

    // synchronized: Pausa a emissão de som e corta a nota se estiver tocando
    public synchronized void pausar() {
        this.tocando = false;
        if (canalMidi != null) {
            canalMidi.allNotesOff(); // Corta qualquer som residual imediatamente
        }
        System.out.println("\n⏸️  [" + nome + "] foi PAUSADO.");
    }

    // synchronized: Retoma a reprodução da faixa
    public synchronized void retomar() {
        this.tocando = true;
        System.out.println("\n▶️  [" + nome + "] voltou a TOCAR.");
    }

    // synchronized: Desliga o instrumento de forma segura
    public synchronized void parar() {
        this.ativo = false;
        this.tocando = false;
        if (canalMidi != null) {
            canalMidi.allNotesOff();
        }
    }

    // synchronized: Altera a velocidade da batida (BPM)
    public synchronized void setBpm(int novoIntervaloMs) {
        this.intervaloMs = novoIntervaloMs;
        System.out.println("\n🎚️  [" + nome + "] novo intervalo: " + novoIntervaloMs + "ms");
    }

    // synchronized: Consulta o estado atual da faixa
    public synchronized boolean isTocando() {
        return this.tocando;
    }

    @Override
    public void run() {
        try {
            // Inicializa o sintetizador de som nativo do computador
            Synthesizer synth = MidiSystem.getSynthesizer();
            synth.open();
            
            // Pega os canais de áudio e define o instrumento escolhido
            MidiChannel[] canais = synth.getChannels();
            canalMidi = canais[canalNumero];
            canalMidi.programChange(instrumentoId); // Aplica o timbre (baixo, synth, etc.)

            // Loop principal da Thread
            while (ativo) {
                if (tocando) {
                    // 1. Imprime na tela
                    System.out.println("[" + nome + "]: " + somTexto);
                    
                    // 2. Toca a nota no alto-falante (nota, volume de 0 a 127)
                    canalMidi.noteOn(notaMidi, 100);
                }

                // Espera o tempo do ritmo (BPM)
                Thread.sleep(intervaloMs);

                // Desliga a nota para a próxima repetição
                if (canalMidi != null) {
                    canalMidi.noteOff(notaMidi);
                }
            }

            synth.close(); // Libera o sintetizador ao encerrar

        } catch (Exception e) {
            System.out.println("❌ Erro no som de [" + nome + "]: " + e.getMessage());
        }

        System.out.println("🛑 [" + nome + "] finalizado.");
    }
}