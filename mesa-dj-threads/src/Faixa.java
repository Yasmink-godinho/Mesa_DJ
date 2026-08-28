import java.io.File; // Localiza e abre o arquivo no sistema operacional


import javax.sound.sampled.AudioInputStream; // Decodifica o arquivo de áudio (.wav)
import javax.sound.sampled.AudioSystem; // Utilitário central de áudio nativo do Java
import javax.sound.sampled.Clip; // Interface para carregar áudio em RAM e controlar reprodução
import javax.sound.sampled.FloatControl; // Controle nativo de parâmetros de áudio (velocidade/sample rate)

public class Faixa  implements Runnable {

    // Atributos visuais e de controle
    private int numeroTecla; // Número da tecla do teclado (ex: 1, 2, 3, 4)
    private String nome;
    private String caminhoArquivo; // Caminho do arquivo de áudio (ex: "sons/bateria.wav")
    private Clip clip; // Objeto dede reprodução de áudio carregado em memória RAM
    private boolean tocando;
    private boolean ativo;
    private int bpm; 

    // Construtor 
    public Faixa(String nome, String caminhoArquivo, int numeroTecla) {
        this.nome = nome;
        this.caminhoArquivo = caminhoArquivo;
        this.numeroTecla = numeroTecla;
        tocando = false;
        ativo = true;
        bpm = 120; // Valor padrão de BPM
    }

    //retorna o número da tecla associada à faixa
    public int getNumeroTecla() {
        return numeroTecla;
    }

    //retorna o nome da faixa
    public String getNome() {
        return nome;
    }

    public synchronized boolean isTocando() {
        return tocando;
    }

    public synchronized int getBpm() {
        return bpm;
    }

     // synchronized: Altera a velocidade da batida (BPM)
    public synchronized void setBpm(int novoBpm) {
        if (novoBpm <= 0) {
            System.out.println("X BPM inválido. Deve ser maior que zero.");
            return;
        }
        bpm = novoBpm;
        System.out.println("\n🎚️ [" + nome + "] novo BPM: " + novoBpm);

        if (clip != null && clip.isOpen()) {
            try{
                if(clip.isControlSupported(FloatControl.Type.SAMPLE_RATE)) { // Verifica se o controle de taxa de amostragem é suportado
                    FloatControl controleVelociada = (FloatControl) clip.getControl(FloatControl.Type.SAMPLE_RATE); // Obtém o controle de taxa de amostragem
                    float taxaOriginal = controleVelociada.getValue(); // Obtém a taxa de amostragem original
                    float fator = (float) novoBpm / 120.0f; // Calcula o fator de ajuste com base no BPM desejado (assumindo 120 BPM como referência)
                    controleVelociada.setValue(taxaOriginal * fator);
                } else {
                    System.out.println("⚠️ Controle de taxa de amostragem não suportado para [" + nome + "].");
                }
            } catch (Exception e) {
                // Caso o driver de áudio não suporte alteração de Sample Rate em tempo real
                System.out.println("ℹ Ajuste de hardware não suportado diretamente, valor lógico atualizado.");
            }
        }
    }

    public synchronized void alternar(){
        if (clip == null){
            return; // Se o clip não estiver carregado, não faz nada
        }

        if (tocando) {
            clip.stop(); // Pausa a reprodução
            tocando = false;
            System.out.println("\n⏸ [Tecla \"" + numeroTecla + "\"] [" + nome + "] foi PAUSADO.");
        } else {
            clip.setFramePosition(0); // Reinicia o áudio do início
            clip.loop(Clip.LOOP_CONTINUOUSLY); // Configura para tocar em loop
            clip.start(); // Inicia a reprodução 
            tocando = true;
            System.out.println("\n▶ [Tecla \"" + numeroTecla + "\"] [" + nome + "] voltou a TOCAR.");
        }
    }
    // synchronized: Pausa a emissão de som e corta a nota se estiver tocando
    public synchronized void pausar() {
       if ( clip != null && clip.isRunning()) {
            clip.stop(); // Pausa a reprodução
            tocando = false;
            System.out.println("\n⏸ [Tecla \"" + numeroTecla + "\"] [" + nome + "] foi PAUSADO.");
        }
    }

    // synchronized: Retoma a reprodução da faixa
    public synchronized void retomar() {
        if (clip != null && !clip.isRunning() && ativo) {
            clip.loop(Clip.LOOP_CONTINUOUSLY); // Configura para tocar em loop
            clip.start(); // Retoma a reprodução
            tocando = true;
            System.out.println("\n▶ [Tecla \"" + numeroTecla + "\"] [" + nome + "] voltou a TOCAR.");
        }
       
    }

    // synchronized: Desliga o instrumento de forma segura
    public synchronized void parar() {
        ativo = false;
        tocando = false;
        if (clip != null) {
            clip.stop(); // Para a reprodução
            clip.close(); // Libera os recursos do clip
             
            System.out.println("\n[Tecla \"" + numeroTecla + "\"] [" + nome + "] foi DESLIGADO.");
        }
    }


    @Override // Implementação do método run() da interface Runnable
    public void run() {
       try {
            File arquivo = new File(caminhoArquivo); // Localiza o arquivo de áudio
            if (!arquivo.exists()) {
                System.err.println("Arquivo de áudio não encontrado: " + caminhoArquivo);
                return;
            }
            // Decodifica o arquivo de áudio na memória ram e prepara para reprodução com latência zero
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(arquivo); // Decodifica o arquivo de áudio
            clip = AudioSystem.getClip(); // Cria um objeto Clip para reprodução
            clip.open(audioStream); // Carrega o áudio no Clip
            System.out.println("\n[Tecla \"" + numeroTecla + "\"] [" + nome + "] carregado com sucesso.");

            while (ativo) {
                Thread.sleep(100); // Mantém o loop ativo enquanto a faixa estiver ativa
            }

        } catch (Exception e) {
            System.err.println("Erro ao carregar a faixa [" + nome + "]: " + e.getMessage()); // Exibe mensagem de erro caso ocorra algum
        }

        
    }
}