# 🎧 Mesa DJ — Mixer Concorrente em Java

Projeto da equipe de **Projeto Integrador**: uma aplicação de console que simula uma mesa de DJ, onde cada faixa musical (instrumento/stem) toca em sua própria **Thread**, de forma independente. O usuário controla tudo por comandos de texto — tocar, pausar, trocar de música, ajustar BPM — sem que uma faixa atrapalhe a outra.

## 🎯 O desafio

- Cada instrumento (Bateria, Baixo, Voz, Sintetizador etc.) roda em uma Thread própria, em loop contínuo.
- É possível pausar e retomar cada faixa individualmente, sem parar o programa nem as demais faixas.
- Toda alteração de estado (tocando/pausado, BPM) é protegida com `synchronized`, pra evitar condições de corrida quando vários comandos chegam ao mesmo tempo.

## ⚙️ Como rodar

Pré-requisitos: **JDK 17+** instalado (com `javac`, não só o `java`).

```bash
cd mesa-dj-threads/src
javac *.java
java App
```

O programa carrega automaticamente a primeira música do catálogo (`Finesse - Bruno Mars`) e já libera o terminal pra receber comandos.

## 🕹️ Comandos disponíveis

| Comando | Ação |
|---|---|
| `1` a `12` | Tocar/retomar a faixa daquele número |
| `p1` a `p12` | Pausar a faixa daquele número |
| `t` | Pausar/continuar **todas** as faixas juntas, mantendo a posição |
| `mute` | Ligar/desligar todas as faixas do zero (master play/mute) |
| `s` | Ver o status atual de todas as faixas |
| `m1`, `m2`, `m3` | Trocar a música carregada |
| `ajuda` | Mostrar os comandos novamente |
| `sair` | Encerrar a aplicação e liberar os recursos de áudio |

## 🧩 Estrutura do projeto

```
mesa-dj-threads/
├── src/
│   ├── App.java              # Ponto de entrada, loop de comandos do usuário
│   ├── MesaDJ.java           # Traduz comandos em ações sobre as Faixas
│   ├── Musica.java           # Agrupa as faixas de uma música e coordena ações globais
│   ├── Faixa.java            # Cada instrumento: Thread própria, controle de play/pause/stop
│   └── testeconcorrencia.java # Testes de estresse com múltiplas threads simultâneas
└── audio/
    └── musica1/               # Stems (.wav) da música carregada por padrão
```

## 🔒 Concorrência e sincronização

- Cada `Faixa` implementa `Runnable` e roda numa `Thread` dedicada, criada em `Musica.carregarFaixas()`.
- Os métodos que alteram estado compartilhado (`tocar`, `pausar`, `retomar`, `parar`, `setBpm`) são `synchronized` dentro de `Faixa`.
- As ações globais (`alternarMaster`, `alternarPausaGlobal`, `ajustarBpmGlobal`) também são `synchronized` em `Musica`, pra garantir consistência quando mexem em várias faixas de uma vez.
- `TesteConcorrencia.java` dispara várias threads batendo comandos simultaneamente na mesma faixa e nos comandos globais, e confere ao final se não houve erro nem inconsistência de estado.

## ✅ Status dos desafios extras

- [x] BPM/velocidade ajustável por faixa e globalmente
- [ ] Painel de status ao vivo (thread dedicada atualizando o console a cada 2s)
- [ ] Comando `add <instrumento>` para adicionar faixa em tempo de execução

## 👥 Equipe — Projeto Integrador

| Pessoa | Responsabilidade |
|---|---|
| Yasmim | Áudio / Instrumento (`Faixa.java`) |
| Lari | Mesa DJ / Comandos (`MesaDJ.java`) |
| Diogo | Threads / Sincronização e testes de concorrência |
| Kezia | Status / Painel visual das faixas |
| Thay | Desafio extra (BPM/velocidade) |
| João Rafael | Integração — GitHub, testes e resolução de conflitos |
| Bela | Documentação / Apresentação |
