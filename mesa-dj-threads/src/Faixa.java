import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.concurrent.CountDownLatch;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class Faixa implements Runnable {

    private int numeroTecla;
    private String nome;
    private String caminhoArquivo;

    // Reprodução de áudio
    private SourceDataLine linhaAudio;
    private byte[] audioBytes;
    private AudioFormat formato;

    // Controle da faixa
    private volatile boolean tocando;
    private volatile boolean ativo;

    // BPM
    private volatile int bpm;

    // Posição atual da música em frames
    private volatile double posicaoFrame;

    // Sincronizador de inicialização
    private CountDownLatch latchPronto;

    // Marca o instante real (nanoTime) correspondente ao valor atual
    // de posicaoFrame — usado para manter a faixa sincronizada
    // enquanto está pausada (ver avancarPosicaoEmSilencio).
    private long ultimoInstante;

    // Só true depois que a faixa tocou pela primeira vez. Antes disso,
    // uma pausa não deve "andar" o tempo — a faixa ainda nem começou,
    // então o certo é continuar do zero na primeira vez que tocar.
    private volatile boolean jaComecouATocar;

    // Construtor
    public Faixa(String nome, String caminhoArquivo, int numeroTecla) {
        this.nome = nome;
        this.caminhoArquivo = caminhoArquivo;
        this.numeroTecla = numeroTecla;

        tocando = false;
        ativo = true;
        bpm = 120;
        posicaoFrame = 0;
    }

    public void setLatchPronto(CountDownLatch latchPronto) {
        this.latchPronto = latchPronto;
    }

    public int getNumeroTecla() {
        return numeroTecla;
    }

    public String getNome() {
        return nome;
    }

    public synchronized boolean isTocando() {
        return tocando;
    }

    public synchronized int getBpm() {
        return bpm;
    }

    // =========================================================
    // CONTROLE DO BPM
    // =========================================================

    public synchronized void setBpm(int novoBpm) {

        if (novoBpm < 40 || novoBpm > 240) {
            System.out.println(
                "X BPM inválido. Use um valor entre 40 e 240."
            );
            return;
        }

        bpm = novoBpm;

        System.out.println(
            "\n[" + nome + "] novo BPM: " + novoBpm
        );
    }

    // =========================================================
    // TOCAR / PAUSAR
    // =========================================================

    public synchronized void alternar() {

        if (linhaAudio == null) {
            return;
        }

        if (tocando) {

            tocando = false;

            linhaAudio.stop();
            linhaAudio.flush();

            System.out.println(
                "\n⏸ [Tecla \"" + numeroTecla + "\"] [" +
                nome + "] foi PAUSADO."
            );

        } else {

            // Alternar começa novamente do início
            posicaoFrame = 0;

            tocando = true;
            jaComecouATocar = true;

            linhaAudio.start();

            System.out.println(
                "\n▶ [Tecla \"" + numeroTecla + "\"] [" +
                nome + "] voltou a TOCAR."
            );
        }
    }

    // =========================================================
    // PAUSAR
    // =========================================================

    public synchronized void pausar() {

        if (linhaAudio != null && tocando) {

            tocando = false;

            linhaAudio.stop();
            linhaAudio.flush();

            System.out.println(
                "\n⏸ [Tecla \"" + numeroTecla + "\"] [" +
                nome + "] foi PAUSADO."
            );
        }
    }

    // =========================================================
    // RETOMAR
    // =========================================================

    public synchronized void retomar() {

        if (linhaAudio != null && !tocando && ativo) {

            tocando = true;
            jaComecouATocar = true;

            linhaAudio.start();

            System.out.println(
                "\n▶ [Tecla \"" + numeroTecla + "\"] [" +
                nome + "] voltou a TOCAR."
            );
        }
    }

    // =========================================================
    // PARAR
    // =========================================================

    public synchronized void parar() {

        ativo = false;
        tocando = false;

        if (linhaAudio != null) {

            linhaAudio.stop();
            linhaAudio.flush();
            linhaAudio.close();

            System.out.println(
                "\n[Tecla \"" + numeroTecla + "\"] [" +
                nome + "] foi DESLIGADO."
            );
        }
    }

    // =========================================================
    // CARREGAMENTO E THREAD
    // =========================================================

    @Override
    public void run() {

        try {

            File arquivo = new File(caminhoArquivo);

            if (!arquivo.exists()) {

                System.err.println(
                    "Arquivo de áudio não encontrado: "
                    + caminhoArquivo
                );

                return;
            }

            // Abre o WAV
            AudioInputStream entrada =
                AudioSystem.getAudioInputStream(arquivo);

            AudioFormat formatoOriginal = entrada.getFormat();

            // Converte para PCM 16 bits
            formato = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                formatoOriginal.getSampleRate(),
                16,
                formatoOriginal.getChannels(),
                formatoOriginal.getChannels() * 2,
                formatoOriginal.getSampleRate(),
                false
            );

            AudioInputStream pcmStream =
                AudioSystem.getAudioInputStream(
                    formato,
                    entrada
                );

            // Carrega todo o áudio em memória
            audioBytes = lerAudio(pcmStream);

            pcmStream.close();
            entrada.close();

            // Cria a linha de reprodução
            DataLine.Info info =
                new DataLine.Info(
                    SourceDataLine.class,
                    formato
                );

            linhaAudio =
                (SourceDataLine) AudioSystem.getLine(info);

            // Buffer maior que o padrão, mas moderado (~150ms de folga)
            // — grande o bastante pra aguentar várias faixas tocando
            // junto sem engasgar, sem exagerar a ponto de aumentar
            // demais a variação de quando cada linha começa a soar.
            int tamanhoBuffer =
                (int) (formato.getSampleRate() * formato.getFrameSize() * 0.15);

            linhaAudio.open(formato, tamanhoBuffer);

            System.out.println(
                "\n[Tecla \"" + numeroTecla + "\"] [" +
                nome + "] carregado com sucesso."
            );

        } catch (Exception e) {

            System.err.println(
                "Erro ao carregar a faixa [" +
                nome + "]: " + e.getMessage()
            );
        } finally {
            if (latchPronto != null) {
                latchPronto.countDown();
            }
        }

        // Começa parado
        ultimoInstante = System.nanoTime();

        while (ativo) {

            if (!tocando) {

                if (jaComecouATocar) {
                    avancarPosicaoEmSilencio();
                }

                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                continue;
            }

            reproduzirBloco();
            ultimoInstante = System.nanoTime();
        }
    }

    // =========================================================
    // SINCRONIA DURANTE A PAUSA
    //
    // Sem isso, pausar "congela" a posição da faixa: ao despausar,
    // ela volta a tocar de onde parou, perdendo exatamente o tempo
    // que ficou pausada em relação às faixas que continuaram.
    // Aqui a posição continua avançando em tempo real mesmo com o
    // áudio mudo, então ao despausar ela já está onde deveria estar
    // — como se nunca tivesse parado.
    // =========================================================

    private void avancarPosicaoEmSilencio() {

        if (formato == null || audioBytes == null) {
            return;
        }

        long agora = System.nanoTime();
        double segundosPassados = (agora - ultimoInstante) / 1_000_000_000.0;
        ultimoInstante = agora;

        int bytesPorFrame = formato.getChannels() * 2;
        int totalFrames = audioBytes.length / bytesPorFrame;

        if (totalFrames == 0) {
            return;
        }

        double velocidade = (double) bpm / 120.0;
        double framesPassados = segundosPassados * formato.getSampleRate() * velocidade;

        posicaoFrame += framesPassados;

        if (posicaoFrame >= totalFrames) {
            posicaoFrame = posicaoFrame % totalFrames;
        }
    }

    // =========================================================
    // LEITURA DO ARQUIVO
    // =========================================================

    private byte[] lerAudio(AudioInputStream stream)
            throws Exception {

        ByteArrayOutputStream saida =
            new ByteArrayOutputStream();

        byte[] buffer = new byte[8192];

        int bytesLidos;

        while ((bytesLidos = stream.read(buffer)) != -1) {

            saida.write(
                buffer,
                0,
                bytesLidos
            );
        }

        return saida.toByteArray();
    }

    // =========================================================
    // REPRODUÇÃO COM VELOCIDADE CONTROLADA
    // =========================================================

    private void reproduzirBloco() {

        int canais = formato.getChannels();

        int bytesPorFrame = canais * 2;

        int totalFrames =
            audioBytes.length / bytesPorFrame;

        if (totalFrames == 0) {
            return;
        }

        // Quantidade de frames que serão enviados
        // para a placa de áudio de cada vez. Maior aqui = menos vezes
        // que a thread precisa acordar pra alimentar a linha, o que
        // ajuda quando várias faixas competem por CPU ao mesmo tempo.
        int framesSaida = 2048;

        byte[] bufferSaida =
            new byte[framesSaida * bytesPorFrame];

        int framesGerados = 0;

        while (
            framesGerados < framesSaida
            && tocando
            && ativo
        ) {

            int frameAtual =
                (int) posicaoFrame;

            int frameProximo =
                frameAtual + 1;

            // Loop da música
            if (frameAtual >= totalFrames) {

                posicaoFrame = 0;

                frameAtual = 0;
                frameProximo = 1;
            }

            if (frameProximo >= totalFrames) {
                frameProximo = 0;
            }

            double fracao =
                posicaoFrame - frameAtual;

            for (int canal = 0; canal < canais; canal++) {

                int indiceAtual =
                    frameAtual * bytesPorFrame
                    + canal * 2;

                int indiceProximo =
                    frameProximo * bytesPorFrame
                    + canal * 2;

                short amostraAtual =
                    lerShort(
                        audioBytes,
                        indiceAtual
                    );

                short amostraProxima =
                    lerShort(
                        audioBytes,
                        indiceProximo
                    );

                // Interpolação entre os frames
                double amostra =
                    amostraAtual
                    + (
                        amostraProxima
                        - amostraAtual
                    ) * fracao;

                short resultado =
                    (short) Math.max(
                        Short.MIN_VALUE,
                        Math.min(
                            Short.MAX_VALUE,
                            Math.round(amostra)
                        )
                    );

                int indiceSaida =
                    framesGerados * bytesPorFrame
                    + canal * 2;

                // PCM little-endian
                bufferSaida[indiceSaida] =
                    (byte) (resultado & 0xFF);

                bufferSaida[indiceSaida + 1] =
                    (byte) ((resultado >> 8) & 0xFF);
            }

            /*
             * BPM de referência:
             *
             * 120 BPM = velocidade normal
             * 240 BPM = 2x mais rápido
             * 60 BPM  = metade da velocidade
             *
             * O fator determina quantos frames do áudio
             * original avançamos a cada frame reproduzido.
             */
            double velocidade =
                (double) bpm / 120.0;

            posicaoFrame += velocidade;

            framesGerados++;
        }

        if (framesGerados > 0 && tocando && ativo) {

            linhaAudio.write(
                bufferSaida,
                0,
                framesGerados * bytesPorFrame
            );
        }
    }

    // =========================================================
    // CONVERSÃO DE BYTES PARA SHORT
    // =========================================================

    private short lerShort(
            byte[] dados,
            int indice) {

        int baixo =
            dados[indice] & 0xFF;

        int alto =
            dados[indice + 1];

        return (short) (
            baixo
            | (alto << 8)
        );
    }
}