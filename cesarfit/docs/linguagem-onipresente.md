# Descricao do dominio e linguagem onipresente

## Visao geral

A GymFlow e uma academia de bairro que hoje controla alunos em planilhas. O sistema
substitui esse controle: a **recepcao** cadastra o **aluno**, vende um **plano**,
recebe os **pagamentos** e controla as **matriculas**; ao efetivar a matricula o sistema gera
a **ficha de treino** do aluno; o **instrutor** acompanha o treino e atende os pedidos de ajuda;
o aluno registra a **execucao** de cada **item de treino**.

## Termos (usados identicamente na conversa, no codigo e nos testes)

| Termo | Significado no dominio | Onde aparece no codigo |
|---|---|---|
| Aluno | Pessoa fisica vinculada a academia, com CPF e e-mail unicos e um objetivo declarado | `domain.matricula.model.Aluno` |
| Objetivo | Meta do aluno: hipertrofia, emagrecimento ou condicionamento | `Objetivo` |
| Plano | Somente dois: Plano Mensal (1 mes, R$ 120,00) e Plano Anual (12 meses, R$ 1320,00) | `Modalidade` |
| Matricula | Vinculo vigente entre aluno e plano, com inicio, fim e situacao (ativa, vencida, cancelada) | `Matricula` |
| Vigencia | Periodo em que a matricula autoriza o acesso | `Matricula.estaVigenteEm` |
| Mensalidade | Valor do plano contratado, recalculado sempre que o plano muda | `Matricula.valorMensal` |
| Politica de preco | Regra que define o valor de cada plano (Mensal, Anual) | `politica.PoliticaDePreco`, `PrecoMensal`, `PrecoAnual` |
| Pagamento | Quitacao do valor devido da matricula (com multa e juros quando em atraso) | `Pagamento` |
| Ficha de treino | Conjunto ordenado de itens de treino, dividido em A, B e C | `treino.model.FichaDeTreino` |
| Item de treino | Exercicio com series, repeticoes e descanso | `ItemDeTreino` |
| Item pendente | Item ainda nao concluido pelo aluno | iterador `IteradorDePendentes` |
| Instrutor | Profissional da sala de treino que atende os pedidos de ajuda do aluno | `SolicitacaoDeAtendimento.instrutor` |

## Regras de negocio principais

1. Idade do aluno: de 16 a 100 anos (16 e 17 anos exigem responsavel legal).
2. CPF e e-mail sao unicos por aluno; o CPF nao muda depois do cadastro.
3. Um aluno nao pode ter duas matriculas ativas ao mesmo tempo.
4. O valor da matricula vem da politica de preco do plano: Mensal R$ 120,00 e Anual R$ 1320,00.
5. Ao trocar de plano o valor e recalculado e salvo; o valor pago e nao usado vira credito.
6. O pagamento deve ser do valor exato devido; so em dinheiro aceita valor maior (com troco).
7. Matricula cancelada nao aceita pagamento nem renovacao.
8. Sem matricula ativa o aluno nao recebe ficha de treino; aluno com matricula ativa nao pode ser excluido.
9. Check-in: um por dia, das 6h as 22h, so com matricula vigente, nao trancada e sem pagamento pendente.
10. A ficha de treino e gerada automaticamente ao efetivar a matricula; a ficha anterior e arquivada.

## Acesso e frequência

| Termo | Significado |
|---|---|
| Credencial | Senha (sempre codificada) usada para entrar no sistema. O aluno entra com o número da matrícula; a equipe entra com o seu número de acesso (ex.: EQ001). |
| Senha de primeiro acesso | Enquanto o aluno não troca a senha, ela é formada pelos 6 primeiros dígitos do CPF. |
| Perfil | Quem entrou: ALUNO (vê a área do aluno) ou EQUIPE (vê recepção e sala de treino). |
| Presença (check-in) | Registro de que o aluno compareceu em um dia. Um por dia e só com matrícula ativa. |
| Sequência (foguinho) | Quantidade de dias seguidos com presença. Continua acesa até o fim do dia seguinte ao último check-in. |
| Treino da semana | Ficha de treino mais recente do aluno, com exercícios, séries e repetições. |

## Ampliacao do dominio

| Termo | Significado |
|---|---|
| Catalogo de exercicios | Lista de exercicios da academia. So exercicios **ativos** entram em fichas novas e em substituicoes. |
| Exercicio em uso | Exercicio que ja aparece em alguma ficha: nao muda de nome/grupo e nao pode ser excluido, apenas desativado. |
| Vaga da ficha | Posicao da ficha definida pelo objetivo (grupo, exercicio preferido, series, repeticoes, descanso). |
| Aviso de vencimento | Regua de 5 etapas: Lembrete (7 dias antes), Vence em breve (3 a 1 dia), Vence hoje, Matricula vencida e Ultimo aviso (ate 15 dias depois). Cada etapa e enviada uma unica vez. |
| Ocorrencia de equipamento | Problema registrado em um equipamento: Aberta, Em andamento, Resolvida (com solucao). |
| Justificativa de cancelamento | Motivo obrigatorio do cancelamento; a data da solicitacao fica registrada. |
| Estorno | Devolucao de um pagamento com motivo. O valor deixa de contar no total pago; nao ha estorno duplicado. |
| Contestacao de cobranca | Pedido do aluno questionando um pagamento. A equipe registra o parecer; se procedente, o pagamento e estornado. |
| Avaliacao fisica | Pedido de horario do aluno (6h-21h, ate 60 dias). A equipe confirma ou propoe outro horario. |
| Substituicao de exercicio | Pedido para trocar um exercicio da ficha. Aprovada, o item recebe outro exercicio ativo do mesmo grupo. |
| Atendimento do instrutor | Pedido de ajuda vinculado a ficha. Aberto, Em atendimento (instrutor assumiu), Concluido (com orientacao). |
| Feedback pos-treino | Percepcao de esforco (1 a 10) e observacoes do dia. Exige check-in e ao menos um exercicio concluido. |

## Planos, cancelamento e divisao de treino

| Termo | Significado |
|---|---|
| Plano Mensal | R$ 120,00, vigencia de 1 mes. |
| Plano Anual | R$ 1320,00, vigencia de 12 meses (equivale a R$ 110,00 por mes). |
| Taxa de cancelamento | Uma mensalidade (R$ 120,00 no Mensal; R$ 110,00 no Anual), cobrada quando o cancelamento e pedido com menos de 30 dias de antecedencia do fim do contrato. |
| Divisao A/B/C | Toda ficha nasce dividida em tres treinos (A, B e C), gerada automaticamente quando a matricula e efetivada. |
| Montar meu treino | O aluno inclui exercicios ativos do catalogo em A, B ou C, escolhendo series (1-10) e repeticoes (1-60); pode ajustar e remover, mas cada treino mantem ao menos um exercicio. |
| Horario redondo | Avaliacao fisica so em horarios cheios ou meia hora (ex.: 14:00, 14:30). |
