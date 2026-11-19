'use strict';

/* =========================================================
   Novas funcionalidades da AREA DO ALUNO
   avisos de vencimento, feedback pos-treino, avaliacao fisica,
   atendimento do instrutor, substituicao de exercicio e contestacao de cobranca.
   Usa api(), avisar(), esc(), dataBR() de app.js e areaAtual/traduzir() de acesso.js.
   ========================================================= */

let pedidosAtuais = null;

/* Avaliacao fisica so em horarios redondos: 06:00, 06:30 ... 20:30 */
const HORARIOS_REDONDOS = [];
for (let h = 6; h < 21; h++) {
  const hh = String(h).padStart(2, '0');
  HORARIOS_REDONDOS.push(`${hh}:00`, `${hh}:30`);
}

function amanhaISO() {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

const ROTULO_ESFORCO = ['', 'Muito leve', 'Leve', 'Leve', 'Moderado', 'Moderado', 'Puxado',
  'Puxado', 'Muito puxado', 'Quase no limite', 'Máximo'];
const SITUACAO_ALUNO = {
  SOLICITADA: 'Aguardando a equipe', NOVO_HORARIO_PROPOSTO: 'Equipe propôs outro horário', CONFIRMADA: 'Confirmada',
  CANCELADA: 'Cancelada', PENDENTE: 'Em análise', APROVADA: 'Aprovada', REJEITADA: 'Rejeitada',
  ABERTO: 'Aguardando instrutor', EM_ATENDIMENTO: 'Em atendimento', CONCLUIDO: 'Concluído',
  ABERTA: 'Em análise', PROCEDENTE: 'Procedente', IMPROCEDENTE: 'Improcedente',
  REALIZADA: 'Realizada', NAO_COMPARECEU: 'Você faltou', EXPIRADA: 'Expirada', CANCELADO: 'Cancelado',
  AGUARDANDO_ALUNO: 'A equipe precisa de uma resposta sua', PARCIALMENTE_PROCEDENTE: 'Parcialmente procedente',
  EM_RECURSO: 'Recurso em análise'
};
const ALERTA_ALUNO = {
  SOBRECARGA: '⚠️ Três treinos seguidos no limite: capriche no descanso e na alimentação.',
  RISCO_DE_LESAO: '⚠️ Sua carga desta semana subiu muito em relação ao mês. Vá com calma!',
  PROGRESSAO_SUGERIDA: '💪 Treinos leves com a ficha completa: peça ao instrutor para aumentar a carga.',
  DOR_RELATADA: '🩺 Avisamos o instrutor sobre a dor; ele vai te procurar com prioridade.'
};
const SELO_ALUNO = {
  SOLICITADA: 'VENCIDA', NOVO_HORARIO_PROPOSTO: 'VENCIDA', PENDENTE: 'VENCIDA', ABERTO: 'VENCIDA', ABERTA: 'VENCIDA',
  EM_ATENDIMENTO: 'VENCIDA', CONFIRMADA: 'ATIVA', APROVADA: 'ATIVA', CONCLUIDO: 'ATIVA', PROCEDENTE: 'ATIVA',
  CANCELADA: 'CANCELADA', REJEITADA: 'CANCELADA', IMPROCEDENTE: 'CANCELADA',
  REALIZADA: 'ATIVA', NAO_COMPARECEU: 'CANCELADA', EXPIRADA: 'CANCELADA', CANCELADO: 'CANCELADA',
  AGUARDANDO_ALUNO: 'VENCIDA', PARCIALMENTE_PROCEDENTE: 'ATIVA', EM_RECURSO: 'VENCIDA'
};

function seloAluno(status) {
  return `<span class="selo ${SELO_ALUNO[status] || ''}">${esc(SITUACAO_ALUNO[status] || status)}</span>`;
}

function quando(iso) {
  if (!iso) return '—';
  const [data, hora = ''] = String(iso).split('T');
  return `${dataBR(data)}${hora ? ' às ' + hora.slice(0, 5) : ''}`;
}

/* ---------- janela do aluno ---------- */

function abrirJanelaAluno(titulo, corpoHtml, rotulo, aoEnviar) {
  const janela = document.getElementById('janela-aluno');
  document.getElementById('conteudo-janela-aluno').innerHTML = `
    <form id="form-janela-aluno" novalidate>
      <h2>${esc(titulo)}</h2>
      ${corpoHtml}
      <div class="acoes-janela">
        <button type="button" class="botao-vidro fantasma" id="btn-fechar-janela-aluno">Voltar</button>
        <button type="submit" class="botao-vidro primario-vidro"><span>${esc(rotulo)}</span></button>
      </div>
    </form>`;
  document.getElementById('btn-fechar-janela-aluno').addEventListener('click', () => janela.close());
  document.getElementById('form-janela-aluno').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    try {
      await aoEnviar(evento.target);
      janela.close();
    } catch (e) {
      avisar(traduzir(e.message), true);
    }
  });
  janela.showModal();
}

async function acaoDoAluno(promessa, mensagem) {
  try {
    await promessa();
    if (mensagem) avisar(mensagem);
    await carregarExtrasDoAluno();
  } catch (e) {
    avisar(traduzir(e.message), true);
  }
}

/* ---------- carga ---------- */

async function carregarExtrasDoAluno() {
  try {
    pedidosAtuais = await api('GET', '/api/area-aluno/solicitacoes');
  } catch (e) {
    avisar(traduzir(e.message), true);
    return;
  }
  desenharAvisos();
  desenharPosTreino();
  desenharPedidos();
  desenharFinanceiro();
}
window.carregarExtrasDoAluno = carregarExtrasDoAluno;

/* ---------- avisos de vencimento ---------- */

function desenharAvisos() {
  const naoLidos = pedidosAtuais.avisos.filter(a => !a.lida);
  document.getElementById('cartao-avisos').innerHTML = naoLidos.map(a => `
    <div class="vidro aviso-vencimento">
      <span class="aviso-icone" aria-hidden="true">⏰</span>
      <div><strong>${esc(a.etapaRotulo || 'Aviso')} · ${a.cobranca ? 'venceu em' : 'vence em'} ${dataBR(a.vencimento)}</strong><p>${esc(a.mensagem.replace('matricula', 'matrícula').replace('recepcao', 'recepção').replace('Ola', 'Olá').replace('amanha', 'amanhã'))}</p></div>
      <button class="botao-vidro fantasma" data-ler-aviso="${a.id}">Entendi</button>
    </div>`).join('');
}

document.getElementById('cartao-avisos').addEventListener('click', (evento) => {
  const botao = evento.target.closest('[data-ler-aviso]');
  if (!botao) return;
  acaoDoAluno(() => api('POST', `/api/area-aluno/avisos/${botao.dataset.lerAviso}/leitura`));
});

/* ---------- feedback pos-treino ---------- */

function desenharPosTreino() {
  const alvo = document.getElementById('cartao-pos');
  const ficha = areaAtual.ficha;
  const feitos = ficha ? ficha.itens.length - ficha.pendentes : 0;
  const presente = areaAtual.frequencia.presenteHoje;
  const historico = pedidosAtuais.feedbacks.slice(0, 5).map(f => `
    <li><span>${dataBR(f.data)}</span>
      <div class="trilho mini"><span style="width:${f.esforco * 10}%"></span></div>
      <b>${f.esforco}/10</b></li>`).join('');

  let corpo;
  if (pedidosAtuais.feedbackDeHojeRegistrado) {
    const hoje = pedidosAtuais.feedbacks[0];
    corpo = `<p class="vazio-vidro">Feedback de hoje registrado ✓ · esforço <b>${hoje.esforco}/10</b> · ${hoje.duracaoMinutos} min · carga <b>${hoje.carga}</b>${hoje.observacao ? ` · “${esc(hoje.observacao)}”` : ''}</p>
      ${ALERTA_ALUNO[hoje.alerta] ? `<p class="dica-vidro">${ALERTA_ALUNO[hoje.alerta]}</p>` : ''}`;
  } else {
    const bloqueio = !presente ? 'Faça o check-in de hoje para liberar o feedback.'
      : feitos < 1 ? 'Conclua ao menos um exercício para registrar o feedback.' : '';
    corpo = `<form id="form-feedback" class="form-vidro" novalidate>
      <label class="campo-vidro">Percepção de esforço: <b id="rotulo-esforco">6 · Puxado</b>
        <input type="range" name="esforco" min="1" max="10" value="6" class="faixa-esforco">
      </label>
      <label class="campo-vidro">Duração do treino (minutos)
        <input type="number" name="duracao" min="10" max="240" value="60">
      </label>
      <label class="campo-vidro"><span><input type="checkbox" name="dor"> Senti dor durante o treino</span></label>
      <label class="campo-vidro oculto" id="campo-local-dor">Onde doeu?
        <input type="text" name="localDaDor" maxlength="80" placeholder="Ex.: joelho direito">
      </label>
      <label class="campo-vidro">Observações (opcional)
        <textarea name="observacao" rows="2" maxlength="500" placeholder="Como você se sentiu?"></textarea>
      </label>
      ${bloqueio ? `<p class="dica-vidro">${bloqueio}</p>` : ''}
      <button type="submit" class="botao-vidro primario-vidro" ${bloqueio ? 'disabled' : ''}><span>Registrar feedback</span></button>
    </form>`;
  }
  alvo.innerHTML = `<h2>Como foi o treino?</h2>
    <p class="legenda">${feitos} exercício(s) concluído(s) na ficha atual.</p>
    ${corpo}
    ${historico ? `<h3 class="sub-vidro">Últimos registros</h3><ul class="historico-esforco">${historico}</ul>` : ''}`;

  const form = document.getElementById('form-feedback');
  if (!form) return;
  form.esforco.addEventListener('input', () => {
    document.getElementById('rotulo-esforco').textContent = `${form.esforco.value} · ${ROTULO_ESFORCO[form.esforco.value]}`;
  });
  form.dor.addEventListener('change', () => {
    document.getElementById('campo-local-dor').classList.toggle('oculto', !form.dor.checked);
  });
  form.addEventListener('submit', (evento) => {
    evento.preventDefault();
    acaoDoAluno(async () => {
      const f = await api('POST', '/api/area-aluno/feedbacks', {
        esforco: Number(form.esforco.value), observacao: form.observacao.value.trim(),
        duracaoMinutos: Number(form.duracao.value), dor: form.dor.checked, localDaDor: form.localDaDor.value.trim()
      });
      avisar(ALERTA_ALUNO[f.alerta] || `Feedback registrado (carga ${f.carga}). Bom descanso!`);
    });
  });
}

/* ---------- avaliacao fisica, atendimento e substituicoes ---------- */

function desenharPedidos() {
  const alvo = document.getElementById('cartao-pedidos');
  const avaliacao = pedidosAtuais.avaliacoes.find(a => a.emAberto);
  const atendimento = pedidosAtuais.atendimentos.find(a => a.status === 'ABERTO' || a.status === 'EM_ATENDIMENTO');
  const ultimoAtendimento = pedidosAtuais.atendimentos.find(a => a.status === 'CONCLUIDO');
  const ultimaRealizada = pedidosAtuais.avaliacoes.find(a => a.status === 'REALIZADA');
  const ficha = areaAtual.ficha;

  const blocoAvaliacao = avaliacao
    ? `<div class="pedido">
        <div class="pedido-topo"><strong>Avaliação física</strong>${seloAluno(avaliacao.status)}</div>
        <p>Pedido para ${quando(avaliacao.dataDesejada)}</p>
        ${avaliacao.dataProposta && avaliacao.status === 'NOVO_HORARIO_PROPOSTO' ? `<p>Novo horário: <b>${quando(avaliacao.dataProposta)}</b></p>` : ''}
        ${avaliacao.dataAgendada ? `<p>Agendada para <b>${quando(avaliacao.dataAgendada)}</b></p>` : ''}
        ${avaliacao.observacao ? `<p class="dica-vidro">${esc(avaliacao.observacao)}</p>` : ''}
        <div class="pedido-acoes">
          ${avaliacao.status === 'NOVO_HORARIO_PROPOSTO' ? `<button class="botao-vidro" data-aceitar-avaliacao="${avaliacao.id}">Aceitar horário</button>
            <button class="botao-vidro fantasma" data-recusar-avaliacao="${avaliacao.id}">Recusar horário</button>` : ''}
          <button class="botao-vidro fantasma" data-cancelar-avaliacao="${avaliacao.id}">Cancelar</button>
        </div>
      </div>`
    : `<form id="form-avaliacao" class="pedido" novalidate>
        <div class="pedido-topo"><strong>Avaliação física</strong></div>
        <div class="data-hora">
          <label class="campo-vidro">Data<input type="date" name="data" min="${amanhaISO()}"></label>
          <label class="campo-vidro">Horário
            <select name="hora">${HORARIOS_REDONDOS.map(h => `<option value="${h}">${h}</option>`).join('')}</select>
          </label>
        </div>
        <p class="dica-vidro">Pelo menos 24h de antecedência. Faltas sem aviso bloqueiam novos agendamentos.</p>
        ${ultimaRealizada ? `<p class="dica-vidro">Última avaliação: IMC <b>${esc(ultimaRealizada.imc)}</b> · ${esc(ultimaRealizada.classificacaoImc)}</p>` : ''}
        <button type="submit" class="botao-vidro"><span>Solicitar avaliação</span></button>
      </form>`;

  const blocoAtendimento = atendimento
    ? `<div class="pedido">
        <div class="pedido-topo"><strong>Atendimento do instrutor</strong>${seloAluno(atendimento.status)}</div>
        <p>${atendimento.exercicio ? `<b>${esc(acentuar(atendimento.exercicio))}</b> · ` : ''}${esc(atendimento.motivo)}</p>
        ${atendimento.instrutor ? `<p class="dica-vidro">${esc(atendimento.instrutor)} está vindo te ajudar.</p>`
          : `<p class="dica-vidro">Você é o <b>${atendimento.posicaoNaFila || '?'}º</b> da fila${atendimento.prioridade === 'ALTA' ? ' (prioridade alta)' : ''}.</p>
             <div class="pedido-acoes"><button class="botao-vidro fantasma" data-cancelar-atendimento="${atendimento.id}">Cancelar pedido</button></div>`}
      </div>`
    : ficha
      ? `<form id="form-atendimento" class="pedido" novalidate>
          <div class="pedido-topo"><strong>Chamar o instrutor</strong></div>
          <label class="campo-vidro">Sobre qual exercício? (opcional)
            <select name="posicao"><option value="">Treino em geral</option>
              ${ficha.itens.map(i => `<option value="${i.posicao}">${esc(acentuar(i.exercicio))}</option>`).join('')}
            </select>
          </label>
          <label class="campo-vidro">Tipo
            <select name="tipo"><option value="DUVIDA_EXECUCAO">Dúvida de execução</option>
              <option value="AJUSTE_DE_EQUIPAMENTO">Ajuste de equipamento</option>
              <option value="DOR_OU_LESAO">Dor ou lesão (prioridade)</option>
              <option value="OUTRO">Outro</option></select>
          </label>
          <label class="campo-vidro">O que você precisa?
            <textarea name="motivo" rows="2" maxlength="500" placeholder="Ex.: dúvida na execução (mín. 10 caracteres)"></textarea>
          </label>
          <button type="submit" class="botao-vidro"><span>Chamar instrutor</span></button>
          ${ultimoAtendimento ? `<p class="dica-vidro">Última orientação (${esc(ultimoAtendimento.instrutor || '')}): ${esc(ultimoAtendimento.orientacao)}</p>
            ${ultimoAtendimento.nota ? `<p class="dica-vidro">Sua nota: ${'★'.repeat(ultimoAtendimento.nota)}</p>`
              : `<button type="button" class="botao-vidro fantasma" data-avaliar-atendimento="${ultimoAtendimento.id}">Avaliar atendimento</button>`}` : ''}
        </form>`
      : '<p class="vazio-vidro">Você poderá chamar o instrutor quando tiver uma ficha de treino.</p>';

  const subs = pedidosAtuais.substituicoes.slice(0, 4);
  const blocoSubs = subs.length
    ? `<div class="pedido"><div class="pedido-topo"><strong>Trocas de exercício</strong></div>
        <ul class="lista-pedidos">${subs.map(s => `<li>
          <div><b>${esc(acentuar(s.exercicioAtual))}</b>${s.exercicioNovo ? ` → ${esc(acentuar(s.exercicioNovo))}` : ''}
            ${s.resposta ? `<p class="dica-vidro">${esc(s.resposta)}</p>` : ''}</div>
          ${seloAluno(s.status)}</li>`).join('')}</ul></div>`
    : '<p class="dica-vidro">Para pedir a troca de um exercício, use o botão “Trocar” no treino da semana.</p>';

  alvo.innerHTML = `<h2>Minhas solicitações</h2><p class="legenda">A equipe responde por aqui.</p>
    <div class="pedidos">${blocoAvaliacao}${blocoAtendimento}${blocoSubs}</div>`;

  const formAvaliacao = document.getElementById('form-avaliacao');
  if (formAvaliacao) {
    formAvaliacao.addEventListener('submit', (evento) => {
      evento.preventDefault();
      if (!formAvaliacao.data.value) {
        avisar('Escolha a data da avaliação.', true);
        return;
      }
      const dataHora = `${formAvaliacao.data.value}T${formAvaliacao.hora.value}`;
      acaoDoAluno(() => api('POST', '/api/area-aluno/avaliacoes', { dataHora }), 'Avaliação solicitada. Aguarde a confirmação.');
    });
  }
  const formAtendimento = document.getElementById('form-atendimento');
  if (formAtendimento) {
    formAtendimento.addEventListener('submit', (evento) => {
      evento.preventDefault();
      const motivo = formAtendimento.motivo.value.trim();
      if (motivo.length < 10) {
        avisar('Descreva o que você precisa (mínimo de 10 caracteres).', true);
        return;
      }
      const posicao = formAtendimento.posicao.value === '' ? null : Number(formAtendimento.posicao.value);
      acaoDoAluno(() => api('POST', '/api/area-aluno/atendimentos', { posicao, motivo, tipo: formAtendimento.tipo.value }), 'Instrutor chamado!');
    });
  }
}

document.getElementById('cartao-pedidos').addEventListener('click', (evento) => {
  const aceitar = evento.target.closest('[data-aceitar-avaliacao]');
  const cancelar = evento.target.closest('[data-cancelar-avaliacao]');
  const recusar = evento.target.closest('[data-recusar-avaliacao]');
  const cancelarAtendimento = evento.target.closest('[data-cancelar-atendimento]');
  const avaliarAtendimento = evento.target.closest('[data-avaliar-atendimento]');
  if (recusar) {
    acaoDoAluno(() => api('POST', `/api/area-aluno/avaliacoes/${recusar.dataset.recusarAvaliacao}/recusa`), 'Horário recusado. A equipe vai responder.');
    return;
  }
  if (cancelarAtendimento) {
    acaoDoAluno(() => api('POST', `/api/area-aluno/atendimentos/${cancelarAtendimento.dataset.cancelarAtendimento}/cancelamento`), 'Pedido cancelado.');
    return;
  }
  if (avaliarAtendimento) {
    const id = avaliarAtendimento.dataset.avaliarAtendimento;
    abrirJanelaAluno('Avaliar atendimento',
      `<p class="legenda">Até 48h após o atendimento. Notas 1 e 2 pedem um comentário.</p>
       <label class="campo-vidro">Nota<select name="nota">${[5, 4, 3, 2, 1].map(n => `<option value="${n}">${'★'.repeat(n)}</option>`).join('')}</select></label>
       <label class="campo-vidro">Comentário<textarea name="comentario" rows="2" maxlength="300"></textarea></label>`,
      'Enviar avaliação',
      async (form) => {
        await api('POST', `/api/area-aluno/atendimentos/${id}/avaliacao`, { nota: Number(form.nota.value), comentario: form.comentario.value.trim() });
        avisar('Obrigado pela avaliação!');
        await carregarExtrasDoAluno();
      });
    return;
  }
  if (aceitar) {
    acaoDoAluno(() => api('POST', `/api/area-aluno/avaliacoes/${aceitar.dataset.aceitarAvaliacao}/aceite`), 'Avaliação confirmada!');
  } else if (cancelar && confirm('Cancelar a solicitação de avaliação física?')) {
    acaoDoAluno(() => api('POST', `/api/area-aluno/avaliacoes/${cancelar.dataset.cancelarAvaliacao}/cancelamento`), 'Solicitação cancelada.');
  }
});

/* botao "Trocar" de cada exercicio do treino da semana */
document.getElementById('cartao-treino').addEventListener('click', (evento) => {
  const botao = evento.target.closest('[data-trocar]');
  if (!botao || !areaAtual || !areaAtual.ficha) return;
  const posicao = Number(botao.dataset.trocar);
  const item = areaAtual.ficha.itens.find(i => i.posicao === posicao);
  abrirJanelaAluno(`Trocar ${item ? acentuar(item.exercicio) : 'exercício'}`,
    `<p class="legenda">A equipe escolhe um exercício do mesmo grupo muscular e mantém séries e repetições.</p>
     <label class="campo-vidro">Por quê?
       <select name="categoria"><option value="DOR_OU_LESAO">Dor ou lesão (prioridade)</option>
         <option value="EQUIPAMENTO_INDISPONIVEL">Equipamento indisponível</option>
         <option value="DIFICULDADE_TECNICA">Dificuldade técnica</option>
         <option value="PREFERENCIA">Preferência (1 por ficha)</option></select>
     </label>
     <label class="campo-vidro">Motivo da troca
       <textarea name="motivo" rows="3" maxlength="500" placeholder="Ex.: sinto dor no ombro (mín. 10 caracteres)"></textarea>
     </label>`,
    'Enviar pedido',
    async (form) => {
      const motivo = form.motivo.value.trim();
      if (motivo.length < 10) throw new Error('Explique o motivo (mínimo de 10 caracteres).');
      const r = await api('POST', '/api/area-aluno/substituicoes', { fichaId: areaAtual.ficha.id, posicao, motivo, categoria: form.categoria.value });
      avisar(r.aprovadaAutomaticamente ? `Troca aprovada na hora: ${r.exercicioNovo} (equipamento interditado).` : 'Pedido de troca enviado à equipe.');
      if (r.aprovadaAutomaticamente) await carregarAreaDoAluno();
      await carregarExtrasDoAluno();
    });
});

/* ---------- financeiro: pagamentos e contestacoes ---------- */

function desenharFinanceiro() {
  const alvo = document.getElementById('cartao-financeiro');
  const m = areaAtual.matricula;
  const pagamentos = m ? m.pagamentos : [];
  const abertas = pedidosAtuais.contestacoes.filter(c => ['ABERTA', 'AGUARDANDO_ALUNO', 'EM_RECURSO'].includes(c.status));

  const linhas = pagamentos.length
    ? `<ul class="lista-pedidos">${pagamentos.map(p => {
        const emAnalise = abertas.some(c => c.matriculaId === m.id && c.indiceDoPagamento === p.indice);
        return `<li>
          <div><b>${esc(p.valor)}</b> · ${esc(p.formaDePagamento)} · ${dataBR(p.data)}
            ${p.estornado ? `<p class="dica-vidro">Estornado em ${dataBR(p.dataDoEstorno)}: ${esc(p.motivoDoEstorno)}</p>`
              : p.parcialmenteEstornado ? `<p class="dica-vidro">Estorno parcial de ${esc(p.valorEstornado)}</p>` : ''}</div>
          ${p.estornado ? '<span class="selo CANCELADA">Estornado</span>'
            : emAnalise ? '<span class="selo VENCIDA">Contestado</span>'
            : `<button class="botao-vidro fantasma" data-contestar="${p.indice}">Contestar</button>`}
        </li>`;
      }).join('')}</ul>`
    : '<p class="vazio-vidro">Nenhum pagamento registrado nesta matrícula.</p>';

  const contestacoes = pedidosAtuais.contestacoes.slice(0, 4);
  alvo.innerHTML = `<h2>Financeiro</h2>
    <p class="legenda">${m ? `Total pago na matrícula nº ${esc(m.id)}: <b>${esc(m.totalPago)}</b>` : 'Sem matrícula.'}</p>
    <div class="financeiro-grade">
      <div><h3 class="sub-vidro">Pagamentos</h3>${linhas}</div>
      <div><h3 class="sub-vidro">Contestações</h3>${contestacoes.length
        ? `<ul class="lista-pedidos">${contestacoes.map(c => `<li>
            <div><b>${esc(c.valor)}</b> · ${esc(c.motivo)}
              ${c.status === 'AGUARDANDO_ALUNO' ? `<p class="dica-vidro">Pergunta da equipe: ${esc(c.pergunta)}</p>
                <button class="botao-vidro" data-responder-contestacao="${c.id}">Responder</button>` : ''}
              ${c.parecer ? `<p class="dica-vidro">Parecer: ${esc(c.parecer)}${c.valorEstornado ? ` · estornado ${esc(c.valorEstornado)}` : ''}</p>` : ''}
              ${(c.status === 'IMPROCEDENTE' || c.status === 'PARCIALMENTE_PROCEDENTE') && !c.recursoUsado
                ? `<button class="botao-vidro fantasma" data-recorrer-contestacao="${c.id}">Recorrer (até 7 dias)</button>` : ''}</div>
            ${seloAluno(c.status)}</li>`).join('')}</ul>`
        : '<p class="dica-vidro">Achou alguma cobrança errada? Use “Contestar” no pagamento.</p>'}</div>
    </div>`;
}

document.getElementById('cartao-financeiro').addEventListener('click', (evento) => {
  const responder = evento.target.closest('[data-responder-contestacao]');
  const recorrer = evento.target.closest('[data-recorrer-contestacao]');
  if (responder || recorrer) {
    const id = (responder || recorrer).dataset[responder ? 'responderContestacao' : 'recorrerContestacao'];
    abrirJanelaAluno(responder ? 'Responder à equipe' : 'Recorrer da decisão',
      `<p class="legenda">${responder ? 'Envie a informação pedida.' : 'O recurso é julgado por outro analista e só pode ser usado uma vez.'}</p>
       <label class="campo-vidro">${responder ? 'Resposta' : 'Justificativa (mín. 20 caracteres)'}
         <textarea name="texto" rows="3" maxlength="500"></textarea></label>`,
      'Enviar',
      async (form) => {
        await api('POST', `/api/area-aluno/contestacoes/${id}/${responder ? 'resposta' : 'recurso'}`, { texto: form.texto.value.trim() });
        avisar(responder ? 'Resposta enviada.' : 'Recurso enviado.');
        await carregarExtrasDoAluno();
      });
    return;
  }
  const botao = evento.target.closest('[data-contestar]');
  if (!botao || !areaAtual.matricula) return;
  const indice = Number(botao.dataset.contestar);
  const pagamento = areaAtual.matricula.pagamentos.find(p => p.indice === indice);
  abrirJanelaAluno('Contestar cobrança',
    `<p class="legenda">${esc(pagamento.valor)} · ${esc(pagamento.formaDePagamento)} · ${dataBR(pagamento.data)}</p>
     <label class="campo-vidro">Por que esta cobrança está errada?
       <textarea name="motivo" rows="3" maxlength="500" placeholder="Ex.: fui cobrado duas vezes (mín. 10 caracteres)"></textarea>
     </label>`,
    'Enviar contestação',
    async (form) => {
      const motivo = form.motivo.value.trim();
      if (motivo.length < 10) throw new Error('Explique o motivo (mínimo de 10 caracteres).');
      await api('POST', '/api/area-aluno/contestacoes', { matriculaId: areaAtual.matricula.id, indiceDoPagamento: indice, motivo });
      avisar('Contestação enviada. A equipe vai analisar.');
      await carregarExtrasDoAluno();
    });
});

/* =========================================================
   Treino A/B/C montado pelo aluno
   ========================================================= */

let catalogoDoAluno = null;

async function catalogo() {
  if (!catalogoDoAluno) catalogoDoAluno = await api('GET', '/api/area-aluno/catalogo');
  return catalogoDoAluno;
}

function aplicarFicha(ficha, mensagem) {
  areaAtual.ficha = ficha;
  desenharTreino();
  if (mensagem) avisar(mensagem);
  carregarExtrasDoAluno();
}

function camposDeCarga(series, repeticoes) {
  return `<div class="data-hora">
    <label class="campo-vidro">Séries (1 a 10)<input type="number" name="series" min="1" max="10" value="${series}"></label>
    <label class="campo-vidro">Repetições (1 a 60)<input type="number" name="repeticoes" min="1" max="60" value="${repeticoes}"></label>
  </div>`;
}

document.getElementById('cartao-treino').addEventListener('click', async (evento) => {
  if (!areaAtual || !areaAtual.ficha) return;
  const aba = evento.target.closest('[data-divisao]');
  const montar = evento.target.closest('[data-montar-treino]');
  const ajustar = evento.target.closest('[data-ajustar]');
  const remover = evento.target.closest('[data-remover]');
  const adicionar = evento.target.closest('[data-adicionar]');

  if (aba) {
    divisaoAtiva = aba.dataset.divisao;
    desenharTreino();
  } else if (montar) {
    montandoTreino = !montandoTreino;
    desenharTreino();
  } else if (ajustar) {
    const posicao = Number(ajustar.dataset.ajustar);
    const item = areaAtual.ficha.itens.find(i => i.posicao === posicao);
    abrirJanelaAluno(`Ajustar ${acentuar(item.exercicio)}`, camposDeCarga(item.series, item.repeticoes), 'Salvar',
      async (form) => {
        const ficha = await api('PUT', `/api/area-aluno/ficha/itens/${posicao}`,
          { series: Number(form.series.value), repeticoes: Number(form.repeticoes.value) });
        aplicarFicha(ficha, 'Exercício ajustado.');
      });
  } else if (remover) {
    const posicao = Number(remover.dataset.remover);
    const item = areaAtual.ficha.itens.find(i => i.posicao === posicao);
    if (!confirm(`Remover "${acentuar(item.exercicio)}" do treino ${item.divisao}?`)) return;
    try {
      aplicarFicha(await api('DELETE', `/api/area-aluno/ficha/itens/${posicao}`), 'Exercício removido.');
    } catch (e) {
      avisar(traduzir(e.message), true);
    }
  } else if (adicionar) {
    const divisao = adicionar.dataset.adicionar;
    let lista;
    try {
      lista = await catalogo();
    } catch (e) {
      avisar(traduzir(e.message), true);
      return;
    }
    const grupos = [...new Set(lista.map(e => e.grupoMuscular))];
    abrirJanelaAluno(`Adicionar ao treino ${divisao}`,
      `<p class="legenda">Escolha um exercício do catálogo da academia.</p>
       <label class="campo-vidro">Exercício
         <select name="exercicio">${grupos.map(g => `<optgroup label="${esc(nomeDoGrupo(g))}">
           ${lista.filter(e => e.grupoMuscular === g).map(e =>
             `<option value="${e.id}">${esc(acentuar(e.nome))}${e.equipamento ? ` · ${esc(acentuar(e.equipamento))}` : ''}</option>`).join('')}
         </optgroup>`).join('')}</select>
       </label>
       ${camposDeCarga(3, 12)}`,
      'Adicionar',
      async (form) => {
        const ficha = await api('POST', '/api/area-aluno/ficha/itens', {
          exercicioId: Number(form.exercicio.value), divisao,
          series: Number(form.series.value), repeticoes: Number(form.repeticoes.value)
        });
        aplicarFicha(ficha, `Exercício adicionado ao treino ${divisao}.`);
      });
  }
});
