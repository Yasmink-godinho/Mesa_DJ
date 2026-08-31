import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;

// Testa se Faixa e Musica se comportam bem quando vários comandos chegam ao mesmo tempo
public class TesteConcorrencia {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== TESTE DE CONCORRENCIA E SINCRONIZACAO ===\n");

        AtomicInteger totalDeErros = new AtomicInteger(0);

        // Usando "musica1" para carregar as faixas reais configuradas
        Musica musica = new Musica(1, "Finesse - Teste", "musica1");
        musica.carregarFaixas();
        MesaDJ mesaDJ = new MesaDJ(musica);

        Thread.sleep(300);

        // Camada 1: várias threads mexendo na mesma faixa
        System.out.println("Camada 1: atacando a Faixa 1...");
        totalDeErros.addAndGet(
                atacarComMultiplasThreads(8, 50, "Faixa1", () -> {
                    if (Math.random() < 0.5) {
                        mesaDJ.tocar(1);
                    } else {
                        mesaDJ.pausar(1);
                    }
                })
        );

        // Camada 2: comandos globais (mexem em todas as faixas de uma vez)
        System.out.println("\nCamada 2: atacando os comandos globais...");
        totalDeErros.addAndGet(
                atacarComMultiplasThreads(6, 30, "Global-Pausa", mesaDJ::alternarPausaTudo)
        );

        totalDeErros.addAndGet(
                atacarComMultiplasThreads(6, 30, "Global-BPM", () -> {
                    int delta = (Math.random() < 0.5) ? 5 : -5;
                    musica.ajustarBpmGlobal(delta);
                })
        );

        boolean estadoConsistente = verificarConsistenciaDasFaixas(musica);

        System.out.println("\n--- RESULTADO ---");
        System.out.println("Erros detectados: " + totalDeErros.get());
        System.out.println("Faixas consistentes: " + (estadoConsistente ? "SIM" : "NAO"));

        mesaDJ.status().forEach((tecla, texto) -> System.out.println("Tecla " + tecla + " -> " + texto));

        if (totalDeErros.get() == 0 && estadoConsistente) {
            System.out.println("\nSUCESSO: nenhuma condicao de corrida detectada.");
        } else {
            System.out.println("\nATENCAO: revisar sincronizacao.");
        }

        mesaDJ.sair();
    }

    // Dispara N threads simultâneas executando a mesma ação M vezes cada
    private static int atacarComMultiplasThreads(int quantidadeDeThreads, int comandosPorThread,
                                                   String nomeDoGrupo, Runnable acao) throws InterruptedException {

        AtomicInteger erros = new AtomicInteger(0);
        CountDownLatch largada = new CountDownLatch(1);
        Thread[] threads = new Thread[quantidadeDeThreads];

        for (int i = 0; i < quantidadeDeThreads; i++) {
            final int id = i;
            threads[i] = new Thread(() -> {
                try {
                    largada.await();
                    for (int c = 0; c < comandosPorThread; c++) {
                        acao.run();
                        Thread.sleep((long) (Math.random() * 5));
                    }
                } catch (Exception e) {
                    erros.incrementAndGet();
                    System.out.println("Erro em " + nomeDoGrupo + "-" + id + ": " + e.getMessage());
                }
            }, nomeDoGrupo + "-" + id);
            threads[i].start();
        }

        largada.countDown();

        for (Thread t : threads) {
            t.join();
        }

        System.out.println("   " + nomeDoGrupo + ": " + (quantidadeDeThreads * comandosPorThread)
                + " comandos, " + erros.get() + " erro(s).");

        return erros.get();
    }

    // Depois de um ataque só com comandos globais, as faixas devem estar no mesmo estado
    private static boolean verificarConsistenciaDasFaixas(Musica musica) {
        long quantasTocando = musica.getFaixas().values().stream()
                .filter(Faixa::isTocando)
                .count();

        long total = musica.getFaixas().values().size();

        return quantasTocando == 0 || quantasTocando == total;
    }
}