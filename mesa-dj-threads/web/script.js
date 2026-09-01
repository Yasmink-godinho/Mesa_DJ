const elCanais = document.getElementById('canais');
const elMusicaSelect = document.getElementById('musica');
const elBpmValor = document.getElementById('bpmValor');
const elStatusConexao = document.getElementById('statusConexao');

async function chamarApi(caminho, opcoes = {}) {
  const resposta = await fetch(caminho, opcoes);
  if (!resposta.ok) throw new Error('Falha na requisição: ' + caminho);
  return resposta;
}

async function carregarMusicas() {
  const resp = await chamarApi('/api/musicas');
  const musicas = await resp.json();
  elMusicaSelect.innerHTML = '';
  for (const musica of musicas) {
    const opcao = document.createElement('option');
    opcao.value = musica.id;
    opcao.textContent = musica.nome;
    elMusicaSelect.appendChild(opcao);
  }
}

// Mantém o <select> sincronizado caso a música tenha sido trocada por outro navegador conectado no mesmo servidor.
function sincronizarSelecaoMusica(nomeMusicaAtual) {
  for (const opcao of elMusicaSelect.options) {
    if (opcao.textContent === nomeMusicaAtual && elMusicaSelect.value !== opcao.value) {
      elMusicaSelect.value = opcao.value;
      break;
    }
  }
}

// Cria ou atualiza os botões de canal sem recriar o DOM inteiro (evita que o botão "pisque" a cada atualização de 2s).
function renderizarCanais(faixas) {
  const teclasAtuais = new Set(faixas.map(f => String(f.tecla)));

  for (const elExistente of [...elCanais.querySelectorAll('.canal')]) {
    if (!teclasAtuais.has(elExistente.dataset.tecla)) {
      elExistente.remove();
    }
  }

  for (const faixa of faixas) {
    let elCanal = elCanais.querySelector(`.canal[data-tecla="${faixa.tecla}"]`);

    if (!elCanal) {
      elCanal = document.createElement('div');
      elCanal.className = 'canal';
      elCanal.dataset.tecla = faixa.tecla;
      elCanal.innerHTML = `
        <div class="canal-cabecalho">
          <span class="canal-tecla">${faixa.tecla}</span>
          <span class="canal-nome"></span>
        </div>
        <div class="canal-onda">
          <span></span><span></span><span></span><span></span><span></span><span></span>
          <span></span><span></span><span></span><span></span><span></span><span></span>
        </div>
        <button class="canal-botao" type="button"></button>
      `;
      elCanal.querySelector('.canal-botao').addEventListener('click', () => {
        const tecla = elCanal.dataset.tecla;
        const estaTocando = elCanal.querySelector('.canal-botao').classList.contains('tocando');
        alternarCanal(tecla, estaTocando);
      });
      elCanais.appendChild(elCanal);
    }

    // Sempre atualiza o nome, mesmo quando o elemento já existia. Depois de trocar de música, a mesma tecla pode representar um instrumento diferente.
    elCanal.querySelector('.canal-nome').textContent = faixa.nome;

    const botao = elCanal.querySelector('.canal-botao');
    botao.classList.toggle('tocando', faixa.tocando);
    botao.textContent = faixa.tocando ? 'Pausar' : 'Tocar';
  }
}

async function alternarCanal(tecla, estaTocando) {
  const acao = estaTocando ? 'pausar' : 'tocar';
  await chamarApi(`/api/${acao}?tecla=${tecla}`, { method: 'POST' });
  atualizarStatus();
}

async function atualizarStatus() {
  try {
    const resp = await chamarApi('/api/status');
    const dados = await resp.json();

    renderizarCanais(dados.faixas);
    elBpmValor.textContent = dados.bpm;
    sincronizarSelecaoMusica(dados.musica);

    elStatusConexao.textContent = 'conectado';
    elStatusConexao.classList.remove('offline');
  } catch (erro) {
    elStatusConexao.textContent = 'servidor não encontrado';
    elStatusConexao.classList.add('offline');
  }
}

elMusicaSelect.addEventListener('change', async () => {
  await chamarApi(`/api/musica?id=${elMusicaSelect.value}`, { method: 'POST' });
  atualizarStatus();
});

document.getElementById('bpmMenos').addEventListener('click', async () => {
  await chamarApi('/api/bpm?delta=-10', { method: 'POST' });
  atualizarStatus();
});

document.getElementById('bpmMais').addEventListener('click', async () => {
  await chamarApi('/api/bpm?delta=10', { method: 'POST' });
  atualizarStatus();
});

document.getElementById('pausarTudo').addEventListener('click', async () => {
  await chamarApi('/api/pausar-tudo', { method: 'POST' });
  atualizarStatus();
});

document.getElementById('mute').addEventListener('click', async () => {
  await chamarApi('/api/mute', { method: 'POST' });
  atualizarStatus();
});

document.getElementById('sair').addEventListener('click', async () => {
  if (!confirm('Encerrar a Mesa DJ?')) return;
  await chamarApi('/api/sair', { method: 'POST' });
  elStatusConexao.textContent = 'encerrado';
});

carregarMusicas().then(atualizarStatus);
setInterval(atualizarStatus, 2000);