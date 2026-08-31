/**
 * MesaDJ (Comandos):
 *
 * Esta classe é o "controlador" da aplicação: ela recebe as ações do
 * usuário (que chegam através de botões da interface) e as traduz em
 * chamadas de método sobre os objetos corretos.
 *
 * A MesaDJ é INTENCIONALMENTE "burra" em relação a áudio e concorrência:
 *  - NÃO decodifica nem reproduz som (isso é feito dentro de Faixa).
 *  - NÃO cria nem inicia Threads (isso é feito dentro de Musica.carregarFaixas()).
 *  - NÃO decide como o painel de status deve ser desenhado.
 *
 * A MesaDJ só sabe três coisas:
 *  1) quais ações existem (tocar, pausar, status, sair, e agora também
 *     os controles globais de tocar/pausar tudo e mutar tudo);
 *  2) qual Faixa (ou conjunto de Faixas, via Musica) cada ação afeta;
 *  3) qual método já pronto deve chamar para executar aquela ação.
 */

import java.util.LinkedHashMap; // Mapa que preserva a ordem de inserção
import java.util.Map; // Interface de mapeamento chave -> valor

public class MesaDJ { // Controla as ações do usuário (botões) e repassa para a Musica/Faixa correta

    private Musica musicaAtual; // Música atualmente carregada

    public MesaDJ(Musica musicaAtual) { // Construtor: recebe a Musica já pronta (Faixas e Threads já iniciadas)
        this.musicaAtual = musicaAtual;
    }

    public void setMusicaAtual(Musica musicaAtual) { // Permite trocar a música ativa depois de a MesaDJ já existir
        this.musicaAtual = musicaAtual;
    }

    public void tocar(int numeroFaixa) { // Ação: TOCAR uma faixa específica (1=Bateria, 2=Baixo, 3=Guitarra, 4=Violão)
        Faixa faixa = buscarFaixa(numeroFaixa);
        if (faixa == null) return; // buscarFaixa() já avisou o erro; aqui só interrompemos o fluxo

        if (faixa.isTocando()) {
            // Feedback pro usuário: clicar em "tocar" numa faixa que já está tocando não deveria fazer nada de errado, mas é bom avisar.
            System.out.println("  Faixa " + numeroFaixa + " já está tocando.");
            return;
        }

        faixa.retomar();
    }

    public void pausar(int numeroFaixa) { // Ação: PAUSAR uma faixa específica.
        Faixa faixa = buscarFaixa(numeroFaixa);
        if (faixa == null) return;

        if (!faixa.isTocando()) {
            System.out.println("  Faixa " + numeroFaixa + " já está pausada.");
            return;
        }

        faixa.pausar();
    }

    // Ação: TOCAR/PAUSAR TUDO (toggle).
    // A própria Musica decide internamente: se alguma faixa está
    // tocando, pausa todas; se todas estão pausadas, retoma todas.
    // A MesaDJ não reimplementa essa decisão — apenas repassa o pedido.
    public void alternarPausaTudo() {
        if (musicaAtual == null) {
            System.out.println(" Erro: nenhuma música carregada.");
            return;
        }
        musicaAtual.alternarPausaGlobal();
    }

    public void alternarMuteTudo() { // Ação: MUTAR/DESMUTAR TUDO (toggle).
        if (musicaAtual == null) {
            System.out.println(" Erro: nenhuma música carregada.");
            return;
        }
        musicaAtual.alternarMaster();
    }

    // Define o BPM de todas as faixas
public void definirBpmGlobal(int novoBpm) {
    if (musicaAtual == null) {
        System.out.println("Erro: nenhuma música carregada.");
        return;
    }

    if (novoBpm < 40 || novoBpm > 240) {
        System.out.println("X BPM inválido. Use um valor entre 40 e 240.");
        return;
    }

    for (Faixa faixa : musicaAtual.getFaixas().values()) {
        faixa.setBpm(novoBpm);
    }

    System.out.println("BPM global definido para " + novoBpm + ".");
}

    // Define o BPM de uma faixa específica
    public void definirBpmFaixa(int numeroFaixa, int novoBpm) {
        Faixa faixa = buscarFaixa(numeroFaixa);

        if (faixa == null) {
            return;
        }

        faixa.setBpm(novoBpm);
    }

    // Aumenta ou diminui o BPM de todas as faixas
    public void ajustarBpmGlobal(int delta) {
        if (musicaAtual == null) {
            System.out.println("Erro: nenhuma música carregada.");
            return;
        }

        musicaAtual.ajustarBpmGlobal(delta);
    }
    
    public Map<Integer, String> status() { // Ação: STATUS — retorna dados crus (tecla -> "Nome: ESTADO"), sem formatação visual
        Map<Integer, String> statusAtual = new LinkedHashMap<>();

        if (musicaAtual == null || musicaAtual.getFaixas() == null) {
            return statusAtual; // devolve mapa vazio em vez de null, para não quebrar quem for exibir
        }

        for (Map.Entry<Integer, Faixa> entry : musicaAtual.getFaixas().entrySet()) {
            Faixa faixa = entry.getValue();
            String estado = faixa.isTocando() ? "TOCANDO" : "PAUSADO";
            statusAtual.put(entry.getKey(), faixa.getNome() + ": " + estado);
        }

        return statusAtual;
    }

    public void sair() { // Ação: SAIR — pede para a Musica parar e liberar todas as faixas
        System.out.println("\n Encerrando a Mesa DJ...");

        if (musicaAtual != null) {
            musicaAtual.pararFaixas();
        }
    }

    // MÉTODO AUXILIAR PRIVADO: Centraliza toda a validação de acesso a uma faixa específica:
    // nenhuma música carregada, mapa de faixas nulo, ou número de
    // tecla inexistente. Evita NullPointerException e mensagens
    // de erro duplicadas espalhadas pelo código.
    private Faixa buscarFaixa(int numeroFaixa) {
        if (musicaAtual == null) {
            System.out.println(" Erro: nenhuma música carregada.");
            return null;
        }

        Map<Integer, Faixa> faixas = musicaAtual.getFaixas();
        if (faixas == null || !faixas.containsKey(numeroFaixa)) {
            System.out.println(" Erro: faixa " + numeroFaixa + " não encontrada.");
            return null;
        }

        return faixas.get(numeroFaixa);
    }
}  