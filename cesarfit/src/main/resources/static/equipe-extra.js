'use strict';

/* =========================================================
   Novas funcionalidades da EQUIPE
   (catalogo de exercicios, solicitacoes dos alunos, equipamentos,
    avisos de vencimento, cancelamento com justificativa, estorno, feedbacks)
   Usa api(), tentar(), avisar(), esc(), dataBR(), abrirJanela(), fecharJanela(),
   estado e recarregarTudo() de app.js.
   ========================================================= */

const NOME_GRUPO = {
  PEITO: 'Peito', COSTAS: 'Costas', PERNAS: 'Pernas', OMBROS: 'Ombros',
  BRACOS: 'Braços', CORE: 'Core', CARDIO: 'Cardio'
};
const NOME_SITUACAO = {
  SOLICITADA: 'Solicitada', NOVO_HORARIO_PROPOSTO: 'Novo horário proposto', CONFIRMADA: 'Confirmada',
  CANCELADA: 'Cancelada', PENDENTE: 'Pendente', APROVADA: 'Aprovada', REJEITADA: 'Rejeitada',
  ABERTO: 'Aberto', EM_ATENDIMENTO: 'Em atendimento', CONCLUIDO: 'Concluído',
  ABERTA: 'Aberta', EM_ANDAMENTO: 'Em andamento', RESOLVIDA: 'Resolvida',
  PROCEDENTE: 'Procedente', IMPROCEDENTE: 'Improcedente',
  AGUARDANDO_PECA: 'Aguardando peça', REALIZADA: 'Realizada', NAO_COMPARECEU: 'Faltou',
  EXPIRADA: 'Expirada', CANCELADO: 'Cancelado', AGUARDANDO_ALUNO: 'Aguardando aluno',
  PARCIALMENTE_PROCEDENTE: 'Parcialmente procedente', EM_RECURSO: 'Em recurso'
};
const NOME_SEVERIDADE = { BAIXA: 'Baixa', MEDIA: 'Média', ALTA: 'Alta', CRITICA: 'Crítica' };
const COR_SEVERIDADE = { BAIXA: 'ATIVA', MEDIA: 'MENSAL', ALTA: 'VENCIDA', CRITICA: 'CANCELADA' };
const NOME_CATEGORIA = {
  DOR_OU_LESAO: 'Dor ou lesão', EQUIPAMENTO_INDISPONIVEL: 'Equipamento indisponível',
  DIFICULDADE_TECNICA: 'Dificuldade técnica', PREFERENCIA: 'Preferência',
  DUVIDA_EXECUCAO: 'Dúvida de execução', AJUSTE_DE_EQUIPAMENTO: 'Ajuste de equipamento', OUTRO: 'Outro',
  COBRANCA_DUPLICADA: 'Cobrança duplicada', VALOR_INCORRETO: 'Valor incorreto', DESISTENCIA: 'Desistência (7 dias)',
  CONTESTACAO_PROCEDENTE: 'Contestação procedente'
};
const NOME_ALERTA = {
  NENHUM: 'Carga adequada', SOBRECARGA: 'Sobrecarga', RISCO_DE_LESAO: 'Risco de lesão',
  PROGRESSAO_SUGERIDA: 'Sugerir progressão', DOR_RELATADA: 'Dor relatada'
};
const COR_ALERTA = { NENHUM: 'ATIVA', PROGRESSAO_SUGERIDA: 'MENSAL', SOBRECARGA: 'VENCIDA', RISCO_DE_LESAO: 'CANCELADA', DOR_RELATADA: 'CANCELADA' };

const NOME_FORMA = { PIX: 'PIX', DINHEIRO: 'Dinheiro', CARTAO_DEBITO: 'Cartão de débito', CARTAO_CREDITO: 'Cartão de crédito', BOLETO: 'Boleto', CARTAO: 'Cartão' };

function reais(texto) {
  return Number(String(texto || '0').replace('R$', '').trim().replace(/\./g, '').replace(',', '.')) || 0;
}

/** Quitada: o pago (mais o crédito de troca) cobre o plano e não há atraso. */
function quitada(m) {
  return m.diasDeAtraso === 0 && reais(m.totalPago) + reais(m.creditoDeTroca) >= m.valorNumerico;
}

function selo(texto, cor) {
  return `<span class="etiqueta ${cor || ''}">${esc(texto)}</span>`;
}
const SITUACAO_COR = {
  SOLICITADA: 'VENCIDA', NOVO_HORARIO_PROPOSTO: 'VENCIDA', PENDENTE: 'VENCIDA', ABERTO: 'VENCIDA',
  ABERTA: 'VENCIDA', EM_ATENDIMENTO: 'MENSAL', EM_ANDAMENTO: 'MENSAL',
  CONFIRMADA: 'ATIVA', APROVADA: 'ATIVA', CONCLUIDO: 'ATIVA', RESOLVIDA: 'ATIVA', PROCEDENTE: 'ATIVA',
  CANCELADA: 'CANCELADA', REJEITADA: 'CANCELADA', IMPROCEDENTE: 'CANCELADA',
  AGUARDANDO_PECA: 'MENSAL', REALIZADA: 'ATIVA', NAO_COMPARECEU: 'CANCELADA', EXPIRADA: 'CANCELADA',
  CANCELADO: 'CANCELADA', AGUARDANDO_ALUNO: 'MENSAL', PARCIALMENTE_PROCEDENTE: 'MENSAL', EM_RECURSO: 'VENCIDA'
};

function situacao(status) {
  return `<span class="etiqueta ${SITUACAO_COR[status] || ''}">${esc(NOME_SITUACAO[status] || status)}</span>`;
}

function dataHoraBR(iso) {
  if (!iso) return '—';
  const [data, hora = ''] = String(iso).split('T');
  return `${dataBR(data)} ${hora.slice(0, 5)}`.trim();
}

function cabecalhoJanela(titulo) {
  return `<h2>${esc(titulo)} <button class="fechar" data-acao="fechar-janela" aria-label="Fechar">×</button></h2>`;
}

/** Abre uma janela com um formulario e executa a acao ao enviar. */
function janelaComFormulario(titulo, dica, camposHtml, rotulo, aoEnviar) {
  abrirJanela(`${cabecalhoJanela(titulo)}
    ${dica ? `<p class="dica">${dica}</p>` : ''}
    <form id="form-janela" novalidate>${camposHtml}
      <button type="submit" class="primario">${esc(rotulo)}</button>
    </form>`);
  document.getElementById('form-janela').addEventListener('submit', (evento) => {
    evento.preventDefault();
    const form = evento.target;
    tentar(async () => {
      await aoEnviar(form);
      fecharJanela();
    });
  });
}

/* ---------- acoes por delegacao (data-x) ---------- */

const ACOES_EXTRA = {};

document.addEventListener('click', (evento) => {
  const botao = evento.target.closest('[data-x]');
  if (!botao) return;
  const acao = ACOES_EXTRA[botao.dataset.x];
  if (acao) tentar(() => acao(Number(botao.dataset.id), botao.dataset));
});

/* =========================================================
   MATRICULAS: detalhes, cancelamento com justificativa e estorno
   ========================================================= */

ACOES['cancelar-matricula'] = (id) => {
  const m = estado.matriculas.find(x => x.id === id);
  janelaComFormulario('Cancelar matrícula',
    `${esc(m ? m.alunoNome : '')} · a data da solicitação (${dataBR(hojeISO())}) será registrada.
     Se faltarem menos de 30 dias para o fim do contrato (${m ? dataBR(m.fim) : ''}), é cobrada a taxa de uma mensalidade.`,
    `<label>Justificativa* (mín. 10 caracteres)<textarea name="texto" rows="4" maxlength="500"></textarea></label>`,
    'Confirmar cancelamento',
    async (form) => {
      const texto = form.texto.value.trim();
      if (texto.length < 10) throw new Error('Informe a justificativa (mínimo de 10 caracteres)');
      const cancelada = await api('POST', `/api/matriculas/${id}/cancelamento`, { texto });
      avisar(`Matrícula cancelada. Taxa de cancelamento: ${cancelada.taxaDeCancelamento}.`);
      await recarregarTudo();
    });
};

ACOES['detalhar-matricula'] = async (id) => {
  const m = await api('GET', `/api/matriculas/${id}`);
  const pagamentos = m.pagamentos.length
    ? `<table><thead><tr><th>Data</th><th>Valor</th><th>Forma</th><th>Situação</th><th></th></tr></thead><tbody>
      ${m.pagamentos.map(p => `<tr>
        <td>${dataBR(p.data)}</td><td><strong>${esc(p.valor)}</strong>
          ${p.multa !== 'R$ 0,00' ? `<div class="contador">multa ${esc(p.multa)} · juros ${esc(p.juros)}</div>` : ''}
          ${p.troco !== 'R$ 0,00' ? `<div class="contador">troco ${esc(p.troco)}</div>` : ''}</td>
        <td>${esc(NOME_FORMA[p.formaDePagamento] || p.formaDePagamento)}${p.parcelas > 1 ? ` · ${p.parcelas}x` : ''}</td>
        <td>${p.estornado
          ? `<span class="etiqueta CANCELADA">Estornado</span><div class="contador">${dataBR(p.dataDoEstorno)} · ${esc(NOME_CATEGORIA[p.categoriaDoEstorno] || '')} · ${esc(p.motivoDoEstorno)}</div>`
          : p.parcialmenteEstornado
            ? `<span class="etiqueta MENSAL">Estorno parcial</span><div class="contador">estornado ${esc(p.valorEstornado)} · saldo ${esc(p.saldoEstornavel)}</div>`
            : '<span class="etiqueta ATIVA">Pago</span>'}</td>
        <td>${p.estornado ? '' : `<button class="btn-mini perigo" data-x="estornar" data-id="${m.id}" data-indice="${p.indice}" data-saldo="${esc(p.saldoEstornavel)}">Estornar</button>`}</td>
      </tr>`).join('')}</tbody></table>`
    : '<p class="vazio">Nenhum pagamento registrado.</p>';
  abrirJanela(`${cabecalhoJanela(`Matrícula #${m.id}`)}
    <p class="dica">${esc(m.alunoNome)} · ${esc(m.planoNome)} · ${esc(m.valorMensal)} · ${dataBR(m.inicio)} a ${dataBR(m.fim)}</p>
    <p><span class="etiqueta ${m.status}">${NOME_STATUS[m.status]}</span> · Total pago: <strong>${esc(m.totalPago)}</strong>
      ${m.pagamentoPendente ? ' <span class="etiqueta CANCELADA">Pagamento pendente</span>' : ''}
      ${m.trancada ? ` <span class="etiqueta MENSAL">Trancada até ${dataBR(m.trancadaAte)}</span>` : ''}</p>
    <p class="dica">${quitada(m) ? '<strong>Pagamento em dia</strong>' : `Valor devido hoje: <strong>${esc(m.valorDevido)}</strong>`}${m.diasDeAtraso > 0 ? ` (${m.diasDeAtraso} dia(s) de atraso: multa ${esc(m.multa)}, juros ${esc(m.juros)})` : ''}
      ${m.creditoDeTroca !== 'R$ 0,00' ? ` · crédito de troca de plano ${esc(m.creditoDeTroca)}` : ''}</p>
    ${m.plano === 'ANUAL' && m.status === 'ATIVA' ? `<div class="cartao"><strong>Trancamento</strong>
      <p class="dica">Usados ${m.diasTrancados} de 60 dias e ${m.trancamentos} de 2 trancamentos neste contrato. Mínimo de 7 dias; exige pagamento em dia.</p>
      ${m.trancada
        ? `<button class="btn-mini" data-x="destrancar" data-id="${m.id}">Destrancar agora (devolve os dias não usados)</button>`
        : `<button class="btn-mini" data-x="trancar" data-id="${m.id}">Trancar matrícula</button>`}</div>` : ''}
    ${m.justificativaDoCancelamento ? `<div class="cartao"><strong>Cancelamento solicitado em ${dataBR(m.dataDaSolicitacaoDeCancelamento)}</strong>
      <p class="dica">${esc(m.justificativaDoCancelamento)}</p>
      <p>Taxa de cancelamento: <strong>${esc(m.taxaDeCancelamento || 'R$ 0,00')}</strong></p></div>` : ''}
    <h3>Pagamentos</h3>${pagamentos}`);
};

ACOES_EXTRA.trancar = (id) => janelaComFormulario('Trancar matrícula',
  'Somente plano Anual. Mínimo 7 dias; até 60 dias e 2 trancamentos por contrato. A vigência é estendida pelos dias trancados e o check-in fica bloqueado.',
  `<label>Dias de trancamento*<input name="dias" type="number" min="7" max="60" value="30"></label>`,
  'Trancar', async (form) => {
    const m = await api('POST', `/api/matriculas/${id}/trancamento`, { dias: Number(form.dias.value) });
    avisar(`Matrícula trancada até ${dataBR(m.trancadaAte)}. Nova vigência: ${dataBR(m.fim)}.`);
    await recarregarTudo();
  });

ACOES_EXTRA.destrancar = async (id) => {
  const m = await api('POST', `/api/matriculas/${id}/destrancamento`);
  fecharJanela();
  avisar(`Matrícula destrancada. Vigência ajustada para ${dataBR(m.fim)}.`);
  await recarregarTudo();
};

ACOES_EXTRA.estornar = (id, dados) => {
  janelaComFormulario('Estornar pagamento',
    `Saldo estornável: <strong>${esc(dados.saldo || '')}</strong>. Prazo: até 90 dias do pagamento.
     Desistência só em até 7 dias (estorno integral e cancela a matrícula sem taxa).
     Duplicidade exige outro pagamento de mesmo valor em até 3 dias.`,
    `<label>Categoria*<select name="categoria">
        <option value="VALOR_INCORRETO">Valor incorreto (aceita parcial)</option>
        <option value="COBRANCA_DUPLICADA">Cobrança duplicada</option>
        <option value="DESISTENCIA">Desistência (direito de arrependimento)</option>
        <option value="OUTRO">Outro</option></select></label>
     <label>Valor a estornar (vazio = saldo integral)<input name="valor" type="text" placeholder="20,00"></label>
     <label>Motivo do estorno* (mín. 10 caracteres)<textarea name="texto" rows="3" maxlength="300"></textarea></label>`,
    'Confirmar estorno',
    async (form) => {
      const motivo = form.texto.value.trim();
      if (motivo.length < 10) throw new Error('Informe o motivo do estorno (mínimo de 10 caracteres)');
      const m = await api('POST', `/api/matriculas/${id}/pagamentos/${dados.indice}/estorno`,
        { motivo, valor: form.valor.value.trim(), categoria: form.categoria.value });
      avisar(m.status === 'CANCELADA' && form.categoria.value === 'DESISTENCIA'
        ? 'Estorno por desistência feito: matrícula cancelada sem taxa.' : `Estorno registrado. Total pago: ${m.totalPago}.`);
      await recarregarTudo();
    });
};

/* ---------- avisos de vencimento ---------- */

async function carregarAvisos() {
  const avisos = await api('GET', '/api/notificacoes');
  const alvo = document.getElementById('lista-avisos');
  alvo.innerHTML = avisos.length
    ? avisos.slice(0, 6).map(a => `<div class="cartao aviso-item">
        ${selo(a.etapaRotulo || 'Aviso', a.cobranca ? 'CANCELADA' : 'VENCIDA')} <strong>${esc(a.alunoNome)}</strong> · vence ${dataBR(a.vencimento)}
        <div class="contador">Enviado ${dataHoraBR(a.enviadaEm)} · ${a.lida ? 'lido pelo aluno' : 'não lido'}</div></div>`).join('')
    : '<p class="vazio">Nenhum aviso enviado ainda.</p>';
}

document.getElementById('btn-verificar-vencimentos').addEventListener('click', () => tentar(async () => {
  const r = await api('POST', '/api/notificacoes/verificacao');
  avisar(r.avisosEnviados ? `${r.avisosEnviados} aviso(s) de vencimento enviado(s).` : 'Nenhum aviso novo: ninguém para avisar hoje.');
  await carregarAvisos();
}));

/* =========================================================
   CATALOGO DE EXERCICIOS
   ========================================================= */

estado.exercicios = [];
estado.editandoExercicioId = null;

async function carregarExercicios() {
  estado.exercicios = await api('GET', '/api/exercicios');
  desenharExercicios();
}

function desenharExercicios() {
  const busca = document.getElementById('busca-exercicio').value.trim().toLowerCase();
  const grupo = document.getElementById('filtro-grupo').value;
  const lista = estado.exercicios.filter(e =>
    (!grupo || e.grupoMuscular === grupo) &&
    (!busca || e.nome.toLowerCase().includes(busca) || (e.equipamento || '').toLowerCase().includes(busca)));
  const ativos = estado.exercicios.filter(e => e.ativo).length;
  document.getElementById('contador-exercicios').textContent = `${ativos} ativo(s) de ${estado.exercicios.length}`;
  const alvo = document.getElementById('tabela-exercicios');
  if (!lista.length) {
    alvo.innerHTML = '<p class="vazio">Nenhum exercício encontrado.</p>';
    return;
  }
  alvo.innerHTML = `<table><thead><tr><th>Exercício</th><th>Grupo</th><th>Equipamento</th><th>Situação</th><th></th></tr></thead><tbody>
    ${lista.map(e => `<tr class="${estado.editandoExercicioId === e.id ? 'em-edicao' : ''} ${e.ativo ? '' : 'inativo'}">
      <td><strong>${esc(acentuar(e.nome))}</strong>${e.usadoEmFichas ? '<div class="contador">usado em fichas</div>' : ''}</td>
      <td>${esc(NOME_GRUPO[e.grupoMuscular] || e.grupoMuscular)}</td>
      <td>${esc(acentuar(e.equipamento || 'Peso corporal'))}</td>
      <td><span class="etiqueta ${e.ativo ? 'ATIVA' : 'CANCELADA'}">${e.ativo ? 'Ativo' : 'Inativo'}</span></td>
      <td class="acoes-tabela acoes-fixas">
        <button class="btn-mini editar" data-x="editar-exercicio" data-id="${e.id}">Editar</button>
        ${e.ativo
          ? `<button class="btn-mini" data-x="desativar-exercicio" data-id="${e.id}">Desativar</button>`
          : `<button class="btn-mini" data-x="reativar-exercicio" data-id="${e.id}">Reativar</button>`}
        ${e.usadoEmFichas ? espacoReservado('Excluir') : `<button class="btn-mini perigo" data-x="excluir-exercicio" data-id="${e.id}">Excluir</button>`}
      </td></tr>`).join('')}
    </tbody></table>`;
}

document.getElementById('busca-exercicio').addEventListener('input', adiar(desenharExercicios, 150));
document.getElementById('filtro-grupo').addEventListener('change', desenharExercicios);

document.getElementById('form-exercicio').addEventListener('submit', (evento) => {
  evento.preventDefault();
  const form = evento.target;
  tentar(async () => {
    const corpo = { nome: form.nome.value.trim(), grupoMuscular: form.grupoMuscular.value, equipamento: form.equipamento.value.trim() };
    if (corpo.nome.length < 3) throw new Error('Informe o nome do exercício (mínimo de 3 caracteres)');
    if (estado.editandoExercicioId) {
      await api('PUT', `/api/exercicios/${estado.editandoExercicioId}`, corpo);
      avisar('Exercício atualizado.');
    } else {
      await api('POST', '/api/exercicios', corpo);
      avisar('Exercício incluído no catálogo.');
    }
    sairEdicaoExercicio();
    await carregarExercicios();
  });
});

function sairEdicaoExercicio() {
  estado.editandoExercicioId = null;
  document.getElementById('form-exercicio').reset();
  document.getElementById('titulo-form-exercicio').textContent = 'Novo exercício';
  document.getElementById('btn-salvar-exercicio').textContent = 'Incluir no catálogo';
  document.getElementById('btn-cancelar-exercicio').classList.add('oculto');
  document.getElementById('aviso-edicao-exercicio').classList.add('oculto');
  desenharExercicios();
}

document.getElementById('btn-cancelar-exercicio').addEventListener('click', sairEdicaoExercicio);

ACOES_EXTRA['editar-exercicio'] = (id) => {
  const e = estado.exercicios.find(x => x.id === id);
  if (!e) return;
  const form = document.getElementById('form-exercicio');
  estado.editandoExercicioId = id;
  form.nome.value = e.nome;
  form.grupoMuscular.value = e.grupoMuscular;
  form.equipamento.value = e.equipamento || '';
  document.getElementById('titulo-form-exercicio').textContent = 'Editar exercício';
  document.getElementById('btn-salvar-exercicio').textContent = 'Salvar alterações';
  document.getElementById('btn-cancelar-exercicio').classList.remove('oculto');
  const aviso = document.getElementById('aviso-edicao-exercicio');
  aviso.textContent = e.usadoEmFichas
    ? `Editando "${e.nome}". Já usado em fichas: nome e grupo não podem mudar.`
    : `Editando "${e.nome}".`;
  aviso.classList.remove('oculto');
  desenharExercicios();
  form.nome.focus();
};

ACOES_EXTRA['desativar-exercicio'] = async (id) => {
  await api('POST', `/api/exercicios/${id}/desativacao`);
  avisar('Exercício desativado: não entra mais em fichas novas.');
  await carregarExercicios();
};

ACOES_EXTRA['reativar-exercicio'] = async (id) => {
  await api('POST', `/api/exercicios/${id}/reativacao`);
  avisar('Exercício reativado.');
  await carregarExercicios();
};

ACOES_EXTRA['excluir-exercicio'] = async (id) => {
  const e = estado.exercicios.find(x => x.id === id);
  if (!confirm(`Excluir "${e ? e.nome : 'exercício'}" do catálogo?`)) return;
  await api('DELETE', `/api/exercicios/${id}`);
  avisar('Exercício excluído.');
  await carregarExercicios();
};

/* =========================================================
   SOLICITACOES DOS ALUNOS
   ========================================================= */

estado.solicitacoes = { avaliacoes: [], atendimentos: [], substituicoes: [], contestacoes: [] };

async function buscarSolicitacoes() {
  const [avaliacoes, atendimentos, substituicoes, contestacoes] = await Promise.all([
    api('GET', '/api/avaliacoes'), api('GET', '/api/atendimentos'),
    api('GET', '/api/substituicoes'), api('GET', '/api/contestacoes')]);
  estado.solicitacoes = { avaliacoes, atendimentos, substituicoes, contestacoes };
  return estado.solicitacoes;
}

function pendencias(s) {
  return {
    avaliacoes: s.avaliacoes.filter(a => a.status === 'SOLICITADA').length,
    atendimentos: s.atendimentos.filter(a => a.status === 'ABERTO' || a.status === 'EM_ATENDIMENTO').length,
    substituicoes: s.substituicoes.filter(a => a.status === 'PENDENTE').length,
    contestacoes: s.contestacoes.filter(a => a.status === 'ABERTA' || a.status === 'EM_RECURSO').length
  };
}

async function contarPendencias() {
  const p = pendencias(await buscarSolicitacoes());
  const total = p.avaliacoes + p.atendimentos + p.substituicoes + p.contestacoes;
  const bolinha = document.getElementById('bolinha-solicitacoes');
  bolinha.textContent = total;
  bolinha.classList.toggle('oculto', total === 0);
}

async function carregarSolicitacoes() {
  const s = await buscarSolicitacoes();
  const p = pendencias(s);
  document.getElementById('indicadores-solicitacoes').innerHTML = `
    <div class="indicador ambar"><span>Avaliações a responder</span><strong>${p.avaliacoes}</strong></div>
    <div class="indicador ambar"><span>Atendimentos em aberto</span><strong>${p.atendimentos}</strong></div>
    <div class="indicador ambar"><span>Substituições pendentes</span><strong>${p.substituicoes}</strong></div>
    <div class="indicador vermelho"><span>Contestações em análise</span><strong>${p.contestacoes}</strong></div>`;
  const total = p.avaliacoes + p.atendimentos + p.substituicoes + p.contestacoes;
  document.getElementById('bolinha-solicitacoes').textContent = total;
  document.getElementById('bolinha-solicitacoes').classList.toggle('oculto', total === 0);

  tabela('tabela-avaliacoes', s.avaliacoes, ['Aluno', 'Pedido para', 'Agendada', 'Situação'], a => `
    <td>${esc(a.alunoNome)}</td><td>${dataHoraBR(a.dataDesejada)}</td>
    <td>${a.dataAgendada ? dataHoraBR(a.dataAgendada) : (a.dataProposta ? `proposta: ${dataHoraBR(a.dataProposta)}` : '—')}
      ${a.imc ? `<div class="contador">IMC ${esc(a.imc)} · ${esc(a.classificacaoImc)}</div>` : ''}</td>
    <td>${situacao(a.status)}${a.status === 'SOLICITADA' ? `<div class="contador">${a.propostasRestantes} proposta(s) restante(s)</div>` : ''}</td>
    <td class="acoes-tabela">${a.status === 'SOLICITADA'
      ? `<button class="btn-mini" data-x="confirmar-avaliacao" data-id="${a.id}">Confirmar</button>
         ${a.propostasRestantes > 0 ? `<button class="btn-mini" data-x="propor-avaliacao" data-id="${a.id}">Propor horário</button>` : ''}`
      : a.status === 'CONFIRMADA' && !a.emAberto
        ? `<button class="btn-mini" data-x="resultado-avaliacao" data-id="${a.id}">Registrar resultado</button>
           <button class="btn-mini perigo" data-x="falta-avaliacao" data-id="${a.id}">Faltou</button>` : ''}</td>`);

  tabela('tabela-atendimentos', s.atendimentos, ['Fila', 'Aluno', 'Pedido', 'Instrutor', 'Situação'], a => `
    <td>${a.posicaoNaFila ? `<strong>${a.posicaoNaFila}º</strong>` : '—'}</td>
    <td>${esc(a.alunoNome)}<div class="contador">${dataHoraBR(a.abertaEm)} · espera ${a.minutosDeEspera} min</div></td>
    <td>${selo(a.prioridade === 'ALTA' ? 'Prioridade alta' : 'Normal', a.prioridade === 'ALTA' ? 'CANCELADA' : '')}
      ${a.automatico ? selo('Aberto pelo sistema', 'MENSAL') : ''}
      ${a.atrasado ? selo('SLA estourado', 'CANCELADA') : ''}<br>
      <span class="contador">${esc(NOME_CATEGORIA[a.tipo] || a.tipo)}</span><br>
      ${a.exercicio ? `<strong>${esc(a.exercicio)}</strong><br>` : ''}${esc(a.motivo)}
      ${a.orientacao ? `<div class="contador">Orientação: ${esc(a.orientacao)}</div>` : ''}
      ${a.nota ? `<div class="contador">Nota do aluno: ${'★'.repeat(a.nota)}${a.comentario ? ` · ${esc(a.comentario)}` : ''}</div>` : ''}</td>
    <td>${esc(a.instrutor || '—')}${a.transferencias ? `<div class="contador">${a.transferencias} transferência(s)</div>` : ''}</td>
    <td>${situacao(a.status)}</td>
    <td class="acoes-tabela">${a.status === 'ABERTO'
      ? `<button class="btn-mini" data-x="assumir-atendimento" data-id="${a.id}">Assumir</button>`
      : a.status === 'EM_ATENDIMENTO'
        ? `<button class="btn-mini" data-x="concluir-atendimento" data-id="${a.id}">Concluir</button>
           <button class="btn-mini" data-x="transferir-atendimento" data-id="${a.id}">Transferir</button>` : ''}</td>`);

  tabela('tabela-substituicoes', s.substituicoes, ['Aluno', 'Exercício', 'Motivo', 'Situação'], a => `
    <td>${esc(a.alunoNome)}</td>
    <td><strong>${esc(a.exercicioAtual)}</strong>${a.exercicioNovo ? ` → ${esc(a.exercicioNovo)}` : ''}
      <div class="contador">${esc(NOME_GRUPO[a.grupoMuscular] || '')}</div></td>
    <td>${selo(NOME_CATEGORIA[a.categoria] || a.categoria, a.categoria === 'DOR_OU_LESAO' ? 'CANCELADA' : '')}<br>${esc(a.motivo)}
      ${a.sugestoes && a.sugestoes.length ? `<div class="contador">Sugestões: ${a.sugestoes.map(esc).join(', ')}</div>` : ''}
      ${a.resposta ? `<div class="contador">Resposta: ${esc(a.resposta)}</div>` : ''}</td>
    <td>${situacao(a.status)}${a.aprovadaAutomaticamente ? '<div class="contador">automática</div>' : ''}</td>
    <td class="acoes-tabela">${a.status === 'PENDENTE'
      ? `<button class="btn-mini" data-x="aprovar-substituicao" data-id="${a.id}">Aprovar</button>
         <button class="btn-mini perigo" data-x="rejeitar-substituicao" data-id="${a.id}">Rejeitar</button>` : ''}</td>`);

  tabela('tabela-contestacoes', s.contestacoes, ['Aluno', 'Cobrança', 'Motivo', 'Situação'], a => `
    <td>${esc(a.alunoNome)}<div class="contador">matrícula #${a.matriculaId}</div></td>
    <td><strong>${esc(a.valor)}</strong></td>
    <td>${esc(a.motivo)}
      ${a.pergunta ? `<div class="contador">Pergunta: ${esc(a.pergunta)}${a.respostaDoAluno ? ` · Resposta: ${esc(a.respostaDoAluno)}` : ''}</div>` : ''}
      ${a.parecer ? `<div class="contador">Parecer (${esc(a.analista || '')}): ${esc(a.parecer)}${a.valorEstornado ? ` · estornado ${esc(a.valorEstornado)}` : ''}</div>` : ''}
      ${a.justificativaDoRecurso ? `<div class="contador">Recurso: ${esc(a.justificativaDoRecurso)}</div>` : ''}</td>
    <td>${situacao(a.status)}${a.atrasada ? `<div>${selo('SLA de 5 dias estourado', 'CANCELADA')}</div>` : ''}</td>
    <td class="acoes-tabela">${a.status === 'ABERTA' || a.status === 'EM_RECURSO'
      ? `<button class="btn-mini" data-x="analisar-contestacao" data-id="${a.id}" data-recurso="${a.status === 'EM_RECURSO'}" data-analista="${esc(a.analista || '')}">${a.status === 'EM_RECURSO' ? 'Julgar recurso' : 'Analisar'}</button>
         <button class="btn-mini" data-x="pedir-info-contestacao" data-id="${a.id}">Pedir informações</button>` : ''}</td>`);
}

function tabela(alvoId, lista, colunas, linha) {
  const alvo = document.getElementById(alvoId);
  alvo.innerHTML = lista.length
    ? `<table><thead><tr>${colunas.map(c => `<th>${c}</th>`).join('')}<th></th></tr></thead>
       <tbody>${lista.map(item => `<tr>${linha(item)}</tr>`).join('')}</tbody></table>`
    : '<p class="vazio">Nenhuma solicitação.</p>';
}

async function depoisDeResponder(mensagem) {
  avisar(mensagem);
  await carregarSolicitacoes();
}

ACOES_EXTRA['confirmar-avaliacao'] = (id) => janelaComFormulario('Confirmar avaliação física',
  'O horário pedido pelo aluno será confirmado.',
  `<label>Observação (opcional)<input name="texto" type="text" maxlength="300" placeholder="Venha com roupa leve"></label>`,
  'Confirmar', async (form) => {
    await api('POST', `/api/avaliacoes/${id}/confirmacao`, { texto: form.texto.value.trim() });
    await depoisDeResponder('Avaliação confirmada.');
  });

ACOES_EXTRA['propor-avaliacao'] = (id) => janelaComFormulario('Propor outro horário',
  'Funcionamento: 6h às 21h, em até 60 dias. O aluno aceita ou cancela pela área dele.',
  `<label>Novo horário* (horários redondos: :00 ou :30)<input name="data" type="datetime-local" step="1800"></label>
   <label>Observação (opcional)<input name="obs" type="text" maxlength="300"></label>`,
  'Enviar proposta', async (form) => {
    if (!form.data.value) throw new Error('Informe o novo horário');
    await api('POST', `/api/avaliacoes/${id}/proposta`, { novaDataHora: form.data.value, observacao: form.obs.value.trim() });
    await depoisDeResponder('Proposta de horário enviada ao aluno.');
  });

ACOES_EXTRA['resultado-avaliacao'] = (id) => janelaComFormulario('Registrar resultado da avaliação',
  'O IMC e a classificação (OMS) são calculados automaticamente.',
  `<label>Peso (kg)*<input name="peso" type="number" step="0.1" min="30" max="300"></label>
   <label>Altura (m)*<input name="altura" type="number" step="0.01" min="1" max="2.5"></label>
   <label>% de gordura (opcional)<input name="gordura" type="number" step="0.1" min="3" max="60"></label>`,
  'Registrar', async (form) => {
    const r = await api('POST', `/api/avaliacoes/${id}/resultado`, {
      pesoKg: Number(form.peso.value) || null, alturaM: Number(form.altura.value) || null,
      percentualDeGordura: form.gordura.value ? Number(form.gordura.value) : null });
    await depoisDeResponder(`Avaliação realizada: IMC ${r.imc} (${r.classificacaoImc}).`);
  });

ACOES_EXTRA['falta-avaliacao'] = async (id) => {
  if (!confirm('Registrar que o aluno não compareceu? Duas faltas em 90 dias bloqueiam novos agendamentos por 30 dias.')) return;
  await api('POST', `/api/avaliacoes/${id}/falta`);
  await depoisDeResponder('Falta registrada.');
};

ACOES_EXTRA['transferir-atendimento'] = (id) => janelaComFormulario('Transferir atendimento',
  'Até 2 transferências por atendimento; o instrutor de destino pode ter no máximo 3 alunos ao mesmo tempo.',
  `<label>Instrutor de destino*<input name="texto" type="text" maxlength="80"></label>`,
  'Transferir', async (form) => {
    await api('POST', `/api/atendimentos/${id}/transferencia`, { texto: form.texto.value.trim() });
    await depoisDeResponder('Atendimento transferido.');
  });

ACOES_EXTRA['pedir-info-contestacao'] = (id) => janelaComFormulario('Pedir informações ao aluno',
  'A contestação fica aguardando o aluno (no máximo 2 pedidos).',
  `<label>Pergunta* (mín. 10 caracteres)<textarea name="texto" rows="3" maxlength="300"></textarea></label>`,
  'Enviar pergunta', async (form) => {
    await api('POST', `/api/contestacoes/${id}/pedido-de-informacoes`, { texto: form.texto.value.trim() });
    await depoisDeResponder('Pergunta enviada ao aluno.');
  });

ACOES_EXTRA['assumir-atendimento'] = async (id) => {
  await api('POST', `/api/atendimentos/${id}/assuncao`);
  await depoisDeResponder('Atendimento assumido.');
};

ACOES_EXTRA['concluir-atendimento'] = (id) => janelaComFormulario('Concluir atendimento', null,
  `<label>Orientação dada ao aluno* (mín. 10 caracteres)<textarea name="texto" rows="4" maxlength="500"></textarea></label>`,
  'Concluir', async (form) => {
    await api('POST', `/api/atendimentos/${id}/conclusao`, { texto: form.texto.value.trim() });
    await depoisDeResponder('Atendimento concluído.');
  });

ACOES_EXTRA['aprovar-substituicao'] = async (id) => {
  const s = estado.solicitacoes.substituicoes.find(x => x.id === id);
  const catalogo = await api('GET', '/api/exercicios');
  const opcoes = catalogo.filter(e => e.ativo && e.grupoMuscular === s.grupoMuscular
    && e.nome.toLowerCase() !== s.exercicioAtual.toLowerCase());
  if (!opcoes.length) throw new Error('Não há outro exercício ativo deste grupo no catálogo');
  janelaComFormulario('Aprovar substituição',
    `${esc(s.alunoNome)} · trocar <strong>${esc(acentuar(s.exercicioAtual))}</strong> (${esc(NOME_GRUPO[s.grupoMuscular] || '')}). Séries e repetições são mantidas.`,
    `<label>Exercício substituto*<select name="exercicio">${opcoes.map(e =>
      `<option value="${e.id}">${esc(acentuar(e.nome))}${e.equipamento ? ` · ${esc(acentuar(e.equipamento))}` : ''}</option>`).join('')}</select></label>
     <label>Observação (opcional)<input name="obs" type="text" maxlength="300"></label>`,
    'Aprovar e trocar na ficha', async (form) => {
      await api('POST', `/api/substituicoes/${id}/aprovacao`, { exercicioId: Number(form.exercicio.value), observacao: form.obs.value.trim() });
      await depoisDeResponder('Substituição aprovada: a ficha do aluno foi atualizada.');
    });
};

ACOES_EXTRA['rejeitar-substituicao'] = (id) => janelaComFormulario('Rejeitar substituição', null,
  `<label>Justificativa* (mín. 10 caracteres)<textarea name="texto" rows="3" maxlength="300"></textarea></label>`,
  'Rejeitar', async (form) => {
    await api('POST', `/api/substituicoes/${id}/rejeicao`, { texto: form.texto.value.trim() });
    await depoisDeResponder('Substituição rejeitada.');
  });

ACOES_EXTRA['analisar-contestacao'] = (id, dados) => janelaComFormulario(
  dados.recurso === 'true' ? 'Julgar recurso' : 'Analisar contestação',
  dados.recurso === 'true'
    ? `O recurso deve ser julgado por um analista diferente de <strong>${esc(dados.analista)}</strong>.`
    : 'Procedente estorna o saldo do pagamento; parcialmente procedente estorna só o valor informado.',
  `<label>Resultado*<select name="resultado"><option value="IMPROCEDENTE">Improcedente</option>
      <option value="PARCIALMENTE_PROCEDENTE">Parcialmente procedente</option>
      <option value="PROCEDENTE">Procedente (estornar)</option></select></label>
   <label>Valor a estornar (só no parcial)<input name="valor" type="text" placeholder="21,00"></label>
   <label>Parecer* (mín. 10 caracteres)<textarea name="texto" rows="4" maxlength="500"></textarea></label>`,
  'Registrar análise', async (form) => {
    await api('POST', `/api/contestacoes/${id}/analise`, { resultado: form.resultado.value,
      valorParcial: form.valor.value.trim(), parecer: form.texto.value.trim() });
    await depoisDeResponder('Análise registrada.');
    await recarregarTudo();
  });

/* =========================================================
   EQUIPAMENTOS
   ========================================================= */

estado.ocorrencias = [];

async function carregarOcorrencias() {
  estado.ocorrencias = await api('GET', '/api/ocorrencias');
  desenharOcorrencias();
}

function desenharOcorrencias() {
  const filtro = document.getElementById('filtro-ocorrencia').value;
  const lista = estado.ocorrencias.filter(o => !filtro || o.status === filtro);
  const pendentes = estado.ocorrencias.filter(o => o.status !== 'RESOLVIDA').length;
  const atrasadas = estado.ocorrencias.filter(o => o.atrasada).length;
  document.getElementById('contador-ocorrencias').textContent =
    `${pendentes} pendente(s)${atrasadas ? ` · ${atrasadas} fora do SLA` : ''}`;
  tabela('tabela-ocorrencias', lista, ['Equipamento', 'Problema', 'SLA', 'Situação'], o => `
    <td><strong>${esc(o.equipamento)}</strong><div class="contador">aberta ${dataHoraBR(o.abertaEm)}</div>
      ${selo(NOME_SEVERIDADE[o.severidade] || o.severidade, COR_SEVERIDADE[o.severidade])}
      ${o.interditado ? selo('Interditado', 'CANCELADA') : ''} ${o.reincidente ? selo('Reincidente', 'VENCIDA') : ''}</td>
    <td>${esc(o.descricao)}
      ${o.pecaAguardada ? `<div class="contador">Peça: ${esc(o.pecaAguardada)}</div>` : ''}
      ${o.solucao ? `<div class="contador">Solução: ${esc(o.solucao)}${o.custoDoReparo ? ` · custo ${esc(o.custoDoReparo)}` : ''}</div>` : ''}
      ${o.reaberturas ? `<div class="contador">${o.reaberturas} reabertura(s)</div>` : ''}</td>
    <td>${o.status === 'RESOLVIDA' ? '—' : `até ${dataHoraBR(o.prazoDoSla)}${o.atrasada ? `<div>${selo('Atrasada', 'CANCELADA')}</div>` : ''}
      ${o.status === 'AGUARDANDO_PECA' ? '<div class="contador">SLA pausado</div>' : ''}`}</td>
    <td>${situacao(o.status)}</td>
    <td class="acoes-tabela">${o.status === 'ABERTA'
      ? `<button class="btn-mini" data-x="iniciar-ocorrencia" data-id="${o.id}">Iniciar</button>`
      : o.status === 'EM_ANDAMENTO'
        ? `<button class="btn-mini" data-x="resolver-ocorrencia" data-id="${o.id}" data-peca="${o.pecaAguardada ? 'sim' : ''}">Resolver</button>
           <button class="btn-mini" data-x="peca-ocorrencia" data-id="${o.id}">Aguardar peça</button>`
        : o.status === 'AGUARDANDO_PECA'
          ? `<button class="btn-mini" data-x="retomar-ocorrencia" data-id="${o.id}">Peça chegou</button>`
          : `<button class="btn-mini" data-x="reabrir-ocorrencia" data-id="${o.id}">Reabrir</button>`}</td>`);
}

document.getElementById('filtro-ocorrencia').addEventListener('change', desenharOcorrencias);

document.getElementById('form-ocorrencia').addEventListener('submit', (evento) => {
  evento.preventDefault();
  const form = evento.target;
  tentar(async () => {
    const corpo = { equipamento: form.equipamento.value.trim(), descricao: form.descricao.value.trim(),
      severidade: form.severidade.value };
    if (!corpo.equipamento) throw new Error('Informe o equipamento');
    if (corpo.descricao.length < 10) throw new Error('Descreva o problema (mínimo de 10 caracteres)');
    await api('POST', '/api/ocorrencias', corpo);
    form.reset();
    avisar('Ocorrência registrada.');
    await carregarOcorrencias();
  });
});

ACOES_EXTRA['iniciar-ocorrencia'] = async (id) => {
  await api('POST', `/api/ocorrencias/${id}/andamento`);
  avisar('Ocorrência em andamento.');
  await carregarOcorrencias();
};

ACOES_EXTRA['resolver-ocorrencia'] = (id, dados) => janelaComFormulario('Resolver ocorrência',
  dados.peca ? 'Houve troca de peça: o custo do reparo é obrigatório.' : null,
  `<label>O que foi feito?* (mín. 5 caracteres)<textarea name="texto" rows="3" maxlength="500"></textarea></label>
   <label>Custo do reparo${dados.peca ? '*' : ' (opcional)'}<input name="custo" type="text" placeholder="89,90"></label>`,
  'Marcar como resolvida', async (form) => {
    await api('POST', `/api/ocorrencias/${id}/resolucao`, { solucao: form.texto.value.trim(), custo: form.custo.value.trim() });
    avisar('Ocorrência resolvida.');
    await carregarOcorrencias();
  });

ACOES_EXTRA['peca-ocorrencia'] = (id) => janelaComFormulario('Aguardar peça',
  'Enquanto a peça não chega, o prazo (SLA) fica pausado.',
  `<label>Peça aguardada*<input name="texto" type="text" maxlength="120"></label>`,
  'Pausar SLA', async (form) => {
    await api('POST', `/api/ocorrencias/${id}/aguardando-peca`, { texto: form.texto.value.trim() });
    avisar('Ocorrência aguardando peça (SLA pausado).');
    await carregarOcorrencias();
  });

ACOES_EXTRA['retomar-ocorrencia'] = async (id) => {
  await api('POST', `/api/ocorrencias/${id}/retomada`);
  avisar('Peça recebida: SLA voltou a contar.');
  await carregarOcorrencias();
};

ACOES_EXTRA['reabrir-ocorrencia'] = (id) => janelaComFormulario('Reabrir ocorrência',
  'Só até 7 dias após a resolução e no máximo 2 vezes. O SLA recomeça.',
  `<label>Motivo* (mín. 10 caracteres)<textarea name="texto" rows="3" maxlength="500"></textarea></label>`,
  'Reabrir', async (form) => {
    await api('POST', `/api/ocorrencias/${id}/reabertura`, { texto: form.texto.value.trim() });
    avisar('Ocorrência reaberta.');
    await carregarOcorrencias();
  });

/* =========================================================
   FEEDBACKS POS-TREINO (consulta na aba Treinos)
   ========================================================= */

async function carregarFeedbacks(alunoId) {
  const alvo = document.getElementById('feedbacks-aluno');
  const lista = await api('GET', `/api/alunos/${alunoId}/feedbacks`);
  alvo.innerHTML = lista.length
    ? `<table><thead><tr><th>Data</th><th>Esforço</th><th>Duração</th><th>Carga</th><th>Alerta</th><th>Exercícios</th><th>Observação</th></tr></thead><tbody>
      ${lista.map(f => `<tr><td>${dataBR(f.data)}</td><td><strong>${f.esforco}/10</strong></td>
        <td>${f.duracaoMinutos} min</td><td>${f.carga}</td>
        <td>${selo(NOME_ALERTA[f.alerta] || f.alerta, COR_ALERTA[f.alerta])}${f.dor ? `<div class="contador">Dor: ${esc(f.localDaDor)}</div>` : ''}</td>
        <td>${f.exerciciosConcluidos}/${f.totalDeExercicios}</td><td>${esc(f.observacao || '—')}</td></tr>`).join('')}
      </tbody></table>`
    : '<p class="vazio">Este aluno ainda não registrou feedbacks.</p>';
}
