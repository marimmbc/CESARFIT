# Prototipos de alta fidelidade

As telas abaixo sao capturas de alta fidelidade do sistema funcionando, com dados de exemplo
(1280 x 860), atualizadas em 19/11/2026. Os arquivos estao em `docs/prototipos/`.
A coluna "Funcionalidade" usa os mesmos nomes da secao 2 do README (numero = integrante).

## Equipe (recepcao e sala de treino)

| Tela | Funcionalidade | Imagem |
|---|---|---|
| Login | 5 - Autenticacao e sessao | ![Login](prototipos/01-login.jpg) |
| Painel | consulta (indicadores, nao contada) | ![Painel](prototipos/02-painel.jpg) |
| Alunos e matriculas - lista | 1 - Gerenciamento de alunos | ![Alunos](prototipos/03-alunos-lista.jpg) |
| Alunos - menor de idade | 1 - Gerenciamento de alunos (responsavel legal; CPF bloqueado na edicao) | ![Menor](prototipos/04-aluno-menor-responsavel.jpg) |
| Matriculas e avisos | 2 - Efetivacao de matricula; 1 - Regua de avisos de vencimento | ![Matriculas](prototipos/05-matriculas-e-avisos.jpg) |
| Registrar pagamento | 3 - Pagamentos | ![Pagamento](prototipos/06-registrar-pagamento.jpg) |
| Detalhe da matricula | 2 - Renovacao, troca de plano e trancamento; 3 - Pagamentos (parcelamento) | ![Trancamento](prototipos/07-matricula-trancamento.jpg) |
| Estornar pagamento | 3 - Estorno | ![Estorno](prototipos/08-estorno.jpg) |
| Treinos - ficha | 4 - Execucao do treino (acompanhamento pela equipe); adicional - geracao automatica da ficha | ![Ficha](prototipos/09-treinos-ficha.jpg) |
| Treinos - feedbacks | 6 - Feedback pos-treino (consulta da equipe) | ![Feedbacks](prototipos/10-treinos-feedbacks.jpg) |
| Exercicios | 4 - Catalogo de exercicios | ![Catalogo](prototipos/11-exercicios.jpg) |
| Solicitacoes | 5 - Avaliacao fisica; 6 - Atendimento do instrutor; 5 - Substituicao de exercicio; 3 - Contestacao | ![Solicitacoes](prototipos/12-solicitacoes.jpg) |
| Analisar contestacao | 3 - Contestacao de cobranca | ![Contestacao](prototipos/13-analisar-contestacao.jpg) |
| Equipamentos | 1 - Ocorrencias de equipamentos | ![Equipamentos](prototipos/14-equipamentos.jpg) |
| Eventos | consulta (trilha de eventos de dominio, nao contada) | ![Eventos](prototipos/15-eventos.jpg) |

## Area do aluno

| Tela | Funcionalidade | Imagem |
|---|---|---|
| Frequencia e meus dados | 6 - Check-in; adicional - troca de senha | ![Frequencia](prototipos/16-aluno-frequencia-dados.jpg) |
| Treino da semana | 4 - Execucao do treino; 4 - Montar o proprio treino; 5 - Substituicao (botao Trocar) | ![Treino](prototipos/17-aluno-treino-semana.jpg) |
| Pos-treino, solicitacoes e financeiro | 6 - Feedback pos-treino; 5 - Avaliacao fisica; 6 - Atendimento do instrutor; 3 - Contestacao | ![Pos-treino](prototipos/18-aluno-pos-treino-solicitacoes-financeiro.jpg) |

## Fluxo navegavel

`Login` -> `Painel` -> `Alunos e matriculas` -> `Detalhe da matricula` (pagar, estornar, trancar)
`Login` -> `Area do aluno` -> `Treino da semana` -> `Pos-treino` -> `Minhas solicitacoes`
