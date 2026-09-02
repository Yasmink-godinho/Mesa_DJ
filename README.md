# 🎧 Mesa DJ — Mixer Concorrente em Java

Projeto desenvolvido para a disciplina de **Infraestrutura de Software**: uma aplicação multithread que simula uma mesa de DJ profissional, onde cada canal de áudio (instrumento/stem) é processado em sua própria **Thread** de forma concorrente e independente. O controle pode ser feito via terminal (CLI) ou interface gráfica web, permitindo tocar, pausar, alternar músicas e regular o BPM em tempo real sem travamentos.


## 🎯 O Desafio & Arquitetura

- **Multithreading Nativo:** Cada stem (Bateria, Baixo, Voz, Sintetizadores, etc.) roda em uma `Thread` própria executando decodificação PCM 16-bit com `SourceDataLine`.
- **Controle Dinâmico de BPM:** Ajuste de velocidade e andamento musical via software através de interpolação linear direta nas amostras de áudio.
- **Sincronização com `CountDownLatch`:** Garante que a interface e o terminal só sejam liberados quando todos os canais de áudio estiverem carregados na memória RAM.
- **Thread Safety:** Todo o ciclo de vida e estado de reprodução é protegido com blocos e métodos `synchronized` e variáveis `volatile`, impedindo condições de corrida (*race conditions*).
- **Padrão MVC:**
  - **Model:** `Musica.java` e `Faixa.java` (gerenciamento de áudio, dados e threads).
  - **Controller:** `MesaDJ.java` (orquestrador de comandos e regras de negócio).
  - **View:** `App.java` (terminal interativo) e `ServidorWeb.java` + pasta `web/` (interface visual para navegador).


## ⚙️ Como Rodar

Pré-requisitos: **JDK 17+** instalado e configurado no terminal.

### Modo 1: Terminal (CLI)

```bash
cd mesa-dj-threads/src
javac *.java
java App

```

### Modo 2: Interface Gráfica Web

Execute o executável batch na raiz do projeto:

```cmd
iniciar-mesa-dj.bat

```

*Ou compile e inicie o servidor manualmente:*

```bash
cd mesa-dj-threads/src
javac *.java
java ServidorWeb

```

Em seguida, abra seu navegador em: **`http://localhost:8080`**


## 🎵 Músicas Disponíveis no Catálogo

1. **`m1` - Finesse (Bruno Mars)**: 18 Stems completos (Drums, Bass, Synths, Vocals, FX, etc.)
2. **`m2` - Dark Horse (Katy Perry)**: 12 Stems
3. **`m3` - Bad Guy (Billie Eilish)**: 14 Stems

O programa carrega automaticamente a primeira música do catálogo (`Finesse - Bruno Mars`) e já libera o terminal pra receber comandos.


## 🕹️ Comandos Disponíveis (CLI)

| Comando | Ação |
| --- | --- |
| `<número>` | Tocar / Retomar o canal individual (ex: `1`, `4`, `12`, `18`) |
| `p<número>` | Pausar o canal individual (ex: `p1`, `p4`, `p12`, `p18`) |
| `bpm+` / `bpm-` | Aumentar / Diminuir o andamento global em ±10 BPM |
| `bpm <valor>` | Definir o BPM exato da música (ex: `bpm 130`) |
| `t` | Pausar / Continuar **todas** as faixas mantendo a posição exata |
| `mute` | Silenciar / Tocar todas as faixas (Master Play/Mute) |
| `s` | Exibir painel de status detalhado de cada canal |
| `m1`, `m2`, `m3` | Trocar a música ativa do catálogo em tempo de execução |
| `ajuda` | Exibir novamente as instruções e atalhos da mesa |
| `sair` | Desligar threads, fechar linhas de áudio e encerrar a aplicação |


## 🧩 Estrutura do Projeto

```text
mesa-dj-threads/
├── audio/
│   ├── musica1/               # 18 stems .wav (Finesse)
│   ├── musica2/               # 12 stems .wav (Dark Horse)
│   └── musica3/               # 14 stems .wav (Bad Guy)
├── src/
│   ├── App.java               # Ponto de entrada CLI (loop de comandos)
│   ├── MesaDJ.java            # Controlador: repassa comandos aos canais
│   ├── Musica.java            # Gerencia catálogo, faixas e sincronizações globais
│   ├── Faixa.java             # Motor de áudio: Thread, PCM e interpolação de BPM
│   ├── ServidorWeb.java       # Backend HTTP para integração com interface web
│   └── TesteConcorrencia.java # Bateria de testes de estresse e concorrência
├── web/
│   ├── index.html             # Painel de controle visual da Mesa DJ
│   ├── style.css              # Estilização moderna estilo launchpad
│   └── script.js              # Chamadas assíncronas para a API Java
└── iniciar-mesa-dj.bat        # Script de inicialização rápida com navegador

```

## 🔒 Concorrência, Sincronização e Robustez

* **Threads por Canal:** Cada `Faixa` implementa `Runnable` e executa em uma `Thread` dedicada (instanciada em `Musica.carregarFaixas()`), rodando em loop contínuo para monitorar a `posicaoFrame` e alimentar buffers na `SourceDataLine`.
* **Inicialização com `CountDownLatch`:** O carregamento assíncrono dos arquivos de áudio bloqueia a thread chamadora até que 100% dos canais estejam instanciados e prontos na memória RAM, prevenindo comandos prematuros em objetos incompletos.
* **Sincronização em Nível de Faixa:** Todos os métodos que manipulam o estado de um canal individual (`tocar`, `pausar`, `retomar`, `parar`, `setBpm`) são protegidos por blocos/métodos `synchronized` e variáveis `volatile`, eliminando condições de corrida.
* **Sincronização Global:** As operações em lote (`alternarMaster`, `alternarPausaGlobal`, `ajustarBpmGlobal`) são centralizadas com `synchronized` dentro de `Musica`, mantendo a consistência do arranjo ao interagir com múltiplos canais em paralelo.
* **Isolamento de Falhas:** Falhas ou exceções no carregamento de um arquivo específico não interrompem nem degradam a execução das demais faixas ativas.
* **Testes de Estresse:** O script `TesteConcorrencia.java` dispara dezenas de threads concorrentes executando comandos massivos e aleatórios de play, pause e ajustes de BPM, auditando o estado final das faixas para validar a integridade e a ausência de deadlocks ou race conditions.


## ✅ Status dos desafios extras

- [x] BPM/velocidade ajustável por faixa e globalmente
- [x] Painel de status ao vivo (thread dedicada atualizando o console a cada 2s)
- [ ] Comando `add <instrumento>` para adicionar faixa em tempo de execução


## 👥 Equipe — Projeto Integrador

| Pessoa | Responsabilidade |
|---|---|
| Yasmin Godinho | Áudio / Instrumento (`Faixa.java`) |
| Larissa Morais | Mesa DJ / Comandos (`MesaDJ.java`) |
| Diogo Alcelino | Threads / Sincronização e testes de concorrência |
| Kezia Aguiar | Status / Painel visual das faixas |
| Thayná Verçosa | Desafio extra (BPM/velocidade) |
| João Rafael | Integração — GitHub, testes e resolução de conflitos |
| Isabela Karla | Documentação  |




