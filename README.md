# CesarFit - Sistema de Gestao de Academia

Projeto da disciplina (trabalho em grupo, 6 integrantes) - CESAR School.
Java 17 + Spring Boot 3 + JPA/Hibernate + H2/PostgreSQL + Cucumber.
Modelado com DDD e arquitetura limpa. Interface web em HTML/CSS/JS puro servida pelo Spring.

> O que mudou nesta versao (Catraca removida, planos Mensal/Anual, CRUD completo de
> alunos e matriculas, dashboard): veja **`ALTERACOES.md`**.

---

## 1. Como executar

### Pre-requisitos
- **JDK 17 ou superior** (`java -version`)
- **Maven 3.9+** (`mvn -v`) — ou use o Maven embutido do VS Code
- VS Code com a extensao **Extension Pack for Java** (Microsoft)

### Rodando no VS Code
1. `File > Open Folder...` e abra a pasta **`cesafit`** (a que contem o `pom.xml`).
2. Aguarde a extensao Java importar o projeto (barra inferior: "Java: Ready").
3. Abra `src/main/java/school/cesar/cesarfit/GymflowApplication.java` e clique em **Run**
   acima do metodo `main` — **ou** no terminal do VS Code:
   ```bash
   mvn spring-boot:run
   ```
4. Acesse **http://localhost:8080** (console do banco H2 em http://localhost:8080/h2,
   JDBC URL `jdbc:h2:mem:gymflow`, usuario `sa`, sem senha).

O banco H2 e em memoria: ao reiniciar, os dados voltam ao estado inicial
(2 alunos de demonstracao: Ana Lima e Bruno Souza).

### Testes automatizados (BDD)
```bash
mvn test        # relatorio em target/cucumber-report.html
```
141 cenarios em `src/test/resources/features` (alunos, matriculas, treinos, operacao, financeiro, solicitacoes...).
O relogio dos testes e controlavel (`RelogioDeTeste`), entao prazos e SLAs sao testados avancando o tempo.

### PostgreSQL (opcional)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gymflow
spring.datasource.username=postgres
spring.datasource.password=postgres
```

---

## 2. Funcionalidades e divisao pelos 6 integrantes

Regra do enunciado (por integrante): 2 altas **ou** 1 media + 2 baixas **ou** 4 baixas.
Toda funcionalidade contada grava dados (nenhuma e so leitura). Listagens, filtros, indicadores do
dashboard e a consulta do proximo exercicio **nao** sao contados separadamente.
As classificacoes de complexidade sao propostas e precisam ser validadas pelo professor.

| Integrante | Criterio | Funcionalidade | Complexidade | Onde esta no codigo | Cenarios BDD |
|---|---|---|---|---|---|
| **1 - Alunos e operacao** | 1 media + 2 baixas | Gerenciamento de alunos com responsavel legal para menores, e-mail unico, idade de 16 a 100 anos e exclusao com anonimizacao (LGPD) | Media | `CadastrarAlunoUseCase`, `AtualizarAlunoUseCase`, `ExcluirAlunoUseCase`, `AlunoController` | `alunos.feature` |
| | | Regua de avisos de vencimento (5 etapas D-7 a D+15, sem repeticao nem regressao, cobranca so sem nova matricula, matricula passa a VENCIDA) | Baixa | `EtapaDeAviso`, `NotificacaoDeVencimento.etapaDevida`, `NotificacaoDeVencimentoUseCase`, `AgendadorDeVencimentos` | `operacao.feature` |
| | | Ocorrencias de equipamentos com severidade, SLA, pausa por peca, interdicao, reabertura e reincidencia | Baixa | `OcorrenciaDeEquipamento`, `Severidade`, `OcorrenciaDeEquipamentoUseCase`, `/api/ocorrencias` | `operacao.feature` |
| **2 - Matriculas** | 2 altas | Gerenciamento completo de matriculas (efetivar, precificar, consultar, editar, excluir) | Alta | `MatricularAlunoUseCase`, `AtualizarMatriculaUseCase`, `ExcluirMatriculaUseCase`, `MatriculaController` | `matriculas.feature` |
| | | Renovacao, alteracao de plano com credito pro-rata e trancamento do plano Anual | Alta | `RenovarMatriculaUseCase`, `AtualizarMatriculaUseCase`, `TrancamentoDeMatriculaUseCase`, `Matricula.trancar` | `matriculas.feature` |
| | | *Adicional:* cancelamento com justificativa, data da solicitacao e taxa de 1 mensalidade se faltarem menos de 30 dias para o fim do contrato | - | `CancelarMatriculaUseCase`, `Matricula.cancelar`, `POST /api/matriculas/{id}/cancelamento` | `financeiro.feature` |
| **3 - Financeiro** | 1 media + 2 baixas | Pagamentos com multa e juros por atraso, troco so em dinheiro, parcelamento do Anual no credito e renovacao ao quitar atraso | Media | `Matricula.registrarPagamento`, `FormaDePagamento`, `RegistrarPagamentoUseCase` | `matriculas.feature` |
| | | Estorno de pagamento total ou parcial com categoria, prazo de 90 dias, duplicidade e direito de arrependimento (cancela sem taxa) | Baixa | `Matricula.estornarPagamento`, `Pagamento`, `MotivoDeEstorno`, `EstornarPagamentoUseCase` | `financeiro.feature` |
| | | Contestacao de cobranca com pedido de informacoes, resultado parcial, SLA, recurso unico por outro analista e limites | Baixa | `ContestacaoDeCobranca`, `StatusContestacao`, `ContestacaoDeCobrancaUseCase` | `financeiro.feature` |
| **4 - Fichas e catalogo** | 2 altas | Geracao automatica de ficha de treino com divisao padrao A/B/C (gerada ja na matricula; usa so exercicios ativos do catalogo) | Alta | `GerarFichaDeTreinoUseCase`, `GeradorDeFicha` e subclasses | `treinos.feature`, `catalogo.feature` |
| | | Gerenciamento do catalogo de exercicios (incluir, editar, desativar, reativar, excluir; regras de duplicidade, uso em fichas e ultimo ativo do grupo) | Alta | `Exercicio`, `CatalogoDeExerciciosUseCase`, `/api/exercicios` | `catalogo.feature` |
| | | *Adicional:* execucao e acompanhamento do treino (feito pelo aluno) | - | `ConcluirItemDeTreinoUseCase`, `ConsultarProximoExercicioUseCase` | `treinos.feature` |
| | | *Adicional:* o aluno monta o proprio treino (escolhe exercicios do catalogo, series e repeticoes; ajusta e remove) | - | `PersonalizarFichaUseCase`, `FichaDeTreino.adicionarExercicio/ajustarItem/removerItem`, `/api/area-aluno/ficha/itens` | `personalizacao.feature` |
| **5 - Acesso e solicitacoes** | 1 media + 2 baixas | Autenticacao com bloqueio apos 5 senhas erradas, senha forte, sessao por inatividade e acesso limitado de matricula vencida | Media | `EntrarNoSistemaUseCase`, `GerenciadorDeSessoes`, `FiltroDeSessao`, `AcessoController` | `acesso.feature` |
| | | Avaliacao fisica: antecedencia de 24h, agenda sem conflito, ate 2 contrapropostas, resultado com IMC, faltas e bloqueio | Baixa | `SolicitacaoDeAvaliacao`, `ResultadoDeAvaliacao`, `AvaliacaoFisicaUseCase` | `solicitacoes.feature` |
| | | Substituicao de exercicio com categorias, limites por ficha, sugestoes automaticas, aprovacao automatica por equipamento interditado e expiracao | Baixa | `SolicitacaoDeSubstituicao`, `MotivoDeSubstituicao`, `SubstituicaoDeExercicioUseCase` | `solicitacoes.feature` |
| | | *Adicional:* alteracao de senha | - | `AlterarSenhaUseCase`, `PUT /api/area-aluno/senha` | `acesso.feature` |
| **6 - Frequencia e treino** | 1 media + 2 baixas | Check-in com horario de funcionamento, bloqueio por inadimplencia e por trancamento | Media | `RegistrarPresencaUseCase`, `Frequencia`, `POST /api/area-aluno/presencas` | `acesso.feature` |
| | | Feedback pos-treino com carga (sRPE), ACWR, alertas de sobrecarga/risco/progressao e dor que aciona o instrutor | Baixa | `FeedbackDeTreino`, `AnaliseDeCarga`, `AlertaDeCarga`, `FeedbackPosTreinoUseCase` | `solicitacoes.feature` |
| | | Atendimento do instrutor com fila priorizada, SLA de resposta, limite por instrutor, transferencia, cancelamento e nota do aluno | Baixa | `SolicitacaoDeAtendimento`, `TipoDeAtendimento`, `AtendimentoDoInstrutorUseCase` | `solicitacoes.feature` |

**Total:** 2 integrantes x 2 altas + 4 integrantes x (1 media + 2 baixas) = **16 funcionalidades contabilizadas**.
Cancelamento, alteracao de senha e execucao do treino ficam como entregas adicionais.

---

## 2.0 Regras das funcionalidades medias e altas (versao ampliada)

### Integrante 1 - Gerenciamento de alunos (media)
| Regra | Detalhe |
|---|---|
| Responsavel legal | aluno de 16 ou 17 anos exige nome e CPF do responsavel (CPF diferente do aluno) |
| Unicidade | CPF e e-mail unicos entre os alunos, no cadastro e na edicao |
| Idade | de 16 a 100 anos |
| Exclusao (LGPD) | sem matricula ativa; sem historico financeiro o aluno e apagado; com pagamentos ele e **anonimizado** (dados pessoais trocados), o historico financeiro fica e ele some das listagens |

### Integrante 2 - Renovacao, troca de plano e trancamento (alta)
| Regra | Detalhe |
|---|---|
| Troca de plano pro-rata | o valor pago e nao usado (por dia) vira credito abatido do novo plano |
| Trancamento | so plano Anual, ativo, vigente e em dia; minimo 7 dias; ate 60 dias e 2 trancamentos por contrato; estende a vigencia e bloqueia o check-in |
| Destrancar antes | os dias nao usados voltam ao saldo e saem da vigencia |

### Integrante 3 - Pagamentos (media)
| Regra | Detalhe |
|---|---|
| Valor devido | plano - credito de troca + multa de 2% + juros de 0,033% ao dia quando pago depois do fim da vigencia |
| Formas | PIX, dinheiro, debito, credito e boleto; so dinheiro aceita valor maior (troco); as demais exigem o valor exato |
| Parcelamento | so cartao de credito e so plano Anual, ate 12x |
| Atraso quitado | o pagamento em atraso renova a vigencia a partir do vencimento anterior |
| Inadimplencia | passou 5 dias do inicio sem pagamento: matricula fica com pagamento pendente (bloqueia check-in e trancamento) |

### Integrante 5 - Autenticacao e sessao (media)
| Regra | Detalhe |
|---|---|
| Forca bruta | 5 senhas erradas seguidas bloqueiam o numero de acesso por 15 minutos |
| Senha forte | minimo 8 caracteres, com letras e numeros, sem conter os digitos do CPF |
| Primeiro acesso | o login avisa que a senha inicial (6 digitos do CPF) deve ser trocada |
| Sessao | expira com 30 minutos sem uso ou 8 horas apos o login |
| Matricula vencida | o aluno entra por ate 30 dias depois do vencimento (para renovar); depois disso, nao |

### Integrante 6 - Check-in (media)
| Regra | Detalhe |
|---|---|
| Horario | das 6h as 22h; um check-in por dia; nao aceita data futura |
| Bloqueios | matricula vigente, nao trancada e sem pagamento pendente |

---

## 2.0.1 Eventos de dominio entre contextos

Os contextos nao chamam mais uns aos outros: o agregado registra um **evento tipado**
(`domain.shared.evento.Evento`), o caso de uso posta no `BarramentoDeEventos` e o contexto interessado reage
com um `ManipuladorDeEvento` (pacote `application.reacao`). Todo evento tambem vai para a trilha de auditoria.

| Evento | Publicado por | Quem reage | Reacao |
|---|---|---|---|
| `MatriculaEfetivada` | Matricula | Treino (`GerarFichaAoEfetivarMatricula`) | gera a ficha A/B/C |
| `DorRelatada` | Treino (feedback) | Atendimento (`AbrirAtendimentoAoRelatarDor`) | abre chamado prioritario |
| `EquipamentoInterditado` / `EquipamentoLiberado` | Operacao (ocorrencia) | Atendimento (`MarcarEquipamentoIndisponivel`, `LiberarEquipamento`) | atualiza a visao propria de equipamentos indisponiveis, usada na troca de exercicio |
| `ContestacaoJulgadaProcedente` | Financeiro (contestacao) | Matricula (`EstornarAoJulgarContestacao`) | estorna o pagamento |

---

## 2.1 Regras das funcionalidades "baixas" (versao ampliada)

As 8 funcionalidades baixas deixaram de ser apenas "muda o status" e passaram a ter regras de negocio
com prazos, calculos, limites e integracao entre contextos. Todas tem cenarios BDD.
Toda regra com prazo recebe o "agora" pela porta `domain.shared.Relogio` (dominio deterministico e testavel).

### Integrante 1 - Regua de avisos de vencimento
| Regra | Detalhe |
|---|---|
| 5 etapas | LEMBRETE (D-7..D-4), PROXIMO_DO_VENCIMENTO (D-3..D-1), VENCE_HOJE (D0), VENCIDA (D+1..D+4), ULTIMO_AVISO (D+5..D+15) |
| Sem repeticao | cada etapa e enviada uma vez por matricula e vencimento (restricao unica `matricula_id, vencimento, etapa`) |
| Sem regressao | se uma etapa posterior ja foi enviada, uma anterior nunca e enviada |
| Verificacao atrasada | envia so a etapa do dia, nao "despeja" as etapas perdidas |
| Cobranca inteligente | VENCIDA/ULTIMO_AVISO nao sao enviados se o aluno ja tem outra matricula vigente; matricula cancelada nao recebe aviso |
| Efeito na matricula | ao entrar na fase de cobranca a matricula passa para VENCIDA |
| Limite | no maximo 1 aviso por aluno por verificacao |

### Integrante 1 - Ocorrencias de equipamentos
| Regra | Detalhe |
|---|---|
| Severidade e SLA | BAIXA 72h, MEDIA 48h, ALTA 24h, CRITICA 4h; ocorrencia fora do prazo aparece como atrasada |
| Interdicao | ALTA e CRITICA interditam o equipamento ate a resolucao (usado pela substituicao de exercicio) |
| Pausa do SLA | ABERTA -> EM_ANDAMENTO <-> AGUARDANDO_PECA: o tempo esperando peca nao conta no prazo |
| Custo | se houve troca de peca, o custo do reparo e obrigatorio na resolucao |
| Reabertura | so ate 7 dias apos resolvida e no maximo 2 vezes; o SLA recomeca |
| Reincidencia | 2 ocorrencias resolvidas do mesmo equipamento em 30 dias: a proxima nasce "reincidente" e com severidade +1 |
| Fila | listagem ordenada: pendentes, atrasadas, mais graves, prazo mais proximo |

### Integrante 3 - Estorno de pagamento
| Regra | Detalhe |
|---|---|
| Parcial ou total | varios estornos parciais ate zerar o saldo; o total pago considera so o saldo nao estornado |
| Prazo | ate 90 dias da data do pagamento (exceto quando vem de contestacao procedente) |
| Categorias | COBRANCA_DUPLICADA, VALOR_INCORRETO, DESISTENCIA, CONTESTACAO_PROCEDENTE, OUTRO |
| Duplicidade | exige outro pagamento ativo de mesmo valor em ate 3 dias e estorno integral |
| Arrependimento | DESISTENCIA so em ate 7 dias (CDC art. 49), integral, e cancela a matricula **sem taxa** |

### Integrante 3 - Contestacao de cobranca
| Regra | Detalhe |
|---|---|
| Prazo para contestar | ate 30 dias do pagamento; so uma contestacao em aberto por pagamento |
| Pedido de informacoes | a equipe pode perguntar ao aluno (max. 2 vezes); aguardando resposta, nao pode decidir |
| Resultados | PROCEDENTE (estorna o saldo), PARCIALMENTE_PROCEDENTE (estorna o valor informado, menor que o cobrado), IMPROCEDENTE |
| SLA | analise em ate 5 dias (da abertura ou do recurso); depois disso aparece atrasada |
| Recurso | uma unica vez, ate 7 dias apos a decisao, julgado por **outro analista** |
| Limite | 2 contestacoes improcedentes em 180 dias bloqueiam novas contestacoes |

### Integrante 5 - Avaliacao fisica
| Regra | Detalhe |
|---|---|
| Horario | futuro, >= 24h de antecedencia, ate 60 dias, 6h-21h, em :00 ou :30 |
| Agenda | um aluno por horario (confirmado ou proposto) |
| Contrapropostas | aluno pode recusar o horario proposto; a equipe propoe no maximo 2 vezes |
| Resultado | so depois do horario; calcula IMC e classificacao (OMS) no objeto de valor `ResultadoDeAvaliacao` |
| Faltas | falta so apos 15 min de tolerancia; 2 faltas em 90 dias bloqueiam agendamentos por 30 dias |
| Intervalo | nova avaliacao so 30 dias apos a ultima realizada; cancelamento de confirmada so ate 2h antes |

### Integrante 5 - Substituicao de exercicio
| Regra | Detalhe |
|---|---|
| Categorias | DOR_OU_LESAO (prioridade maxima), EQUIPAMENTO_INDISPONIVEL, DIFICULDADE_TECNICA, PREFERENCIA |
| Limites | 3 trocas aprovadas por ficha; PREFERENCIA so 1 por ficha |
| Sugestoes | o sistema sugere substitutos: mesmo grupo, ativos, fora da ficha, equipamento nao interditado, equipamento diferente primeiro |
| Aprovacao automatica | EQUIPAMENTO_INDISPONIVEL com o equipamento interditado por ocorrencia e aprovado na hora com a melhor sugestao |
| Validacoes | substituto do mesmo grupo, que nao esteja na ficha e sem equipamento interditado |
| Expiracao e fila | pedido pendente expira em 7 dias; fila da equipe ordenada por prioridade e antiguidade |

### Integrante 6 - Feedback pos-treino
| Regra | Detalhe |
|---|---|
| Dados | esforco 1-10, duracao 10-240 min, dor (com local obrigatorio) e observacao |
| Carga | sRPE = esforco x duracao |
| ACWR | carga aguda (7 dias) / carga cronica (media semanal de 28 dias), com pelo menos 4 registros; > 1,5 = RISCO_DE_LESAO |
| Tendencia | 3 treinos seguidos com esforco >= 9 = SOBRECARGA; 3 treinos com esforco <= 3 e ficha completa = PROGRESSAO_SUGERIDA |
| Dor | gera alerta DOR_RELATADA e **abre automaticamente** um atendimento de prioridade ALTA para o instrutor |

### Integrante 6 - Atendimento do instrutor
| Regra | Detalhe |
|---|---|
| Prioridade | DOR_OU_LESAO = ALTA; demais = NORMAL; fila ordenada por prioridade e depois por ordem de chegada (posicao exibida ao aluno) |
| Ordem | nao e possivel assumir um NORMAL enquanto houver ALTA aguardando |
| SLA | primeira resposta em 10 min (ALTA) ou 30 min (NORMAL) |
| Capacidade | cada instrutor atende no maximo 3 alunos ao mesmo tempo (vale para transferencias) |
| Transferencia | para outro instrutor, no maximo 2 vezes |
| Aluno | cancela enquanto nao foi assumido; avalia de 1 a 5 uma unica vez, ate 48h, comentario obrigatorio para nota 1 ou 2 |

---

## 3. Regras de negocio principais

- Existem **somente dois planos**: **Plano Mensal = R$ 120,00** (vigencia 1 mes) e
  **Plano Anual = R$ 1320,00** (vigencia 12 meses). Nao ha outros planos nem adicionais.
- O valor **nunca vem da tela**: o servidor calcula pelo plano (Strategy) no cadastro **e** na edicao,
  e grava o valor na matricula.
- Um aluno so pode ter **uma matricula ativa** por vez.
- Aluno com matricula ativa **nao pode ser excluido**; com matriculas encerradas (canceladas/vencidas),
  o historico dele e removido junto, sem deixar registros orfaos.
- Validacoes com mensagens claras: CPF (11 digitos, unico), telefone (DDD + numero), e-mail,
  data de nascimento (idade minima 16), aluno e plano obrigatorios na matricula, data de inicio
  obrigatoria, pagamento nao pode ser menor que o valor do plano.

---

## 4. Padroes de projeto

| Padrao | Implementacao | Problema que resolve |
|---|---|---|
| **Strategy** | `politica.PoliticaDePreco` + `PrecoMensal`, `PrecoAnual`, escolhidos por `SeletorDePolitica` | calcular o valor do plano em um unico lugar, sem `if` espalhado |
| **Observer** | `shared.PublicadorDeEventos` + `NotificadorPorEmail`, `TrilhaDeAuditoria` | reagir a eventos de dominio sem acoplar os casos de uso |
| **Template Method** | `treino.servico.GeradorDeFicha` + `GeradorHipertrofia`, `GeradorEmagrecimento`, `GeradorCondicionamento` | fixar o esqueleto da geracao da ficha variando os passos |
| **Factory Method** (extra) | `FabricaDeGerador.para(objetivo)` | escolher o gerador de ficha pelo objetivo |
| **Iterator** | `FichaDeTreino.iterator()` / `IteradorDePendentes` | percorrer apenas os itens pendentes sem expor a colecao |

---

## 5. Arquitetura

```
presentation  ->  application  ->  domain  <-  infrastructure
  (REST)          (casos de uso)   (regras)     (JPA, observers, beans)
```

```
src/main/java/school/cesar/gymflow
├── domain           # regras de negocio puras (sem framework)
│   ├── shared       # Dinheiro, Cpf, eventos, Observer
│   ├── matricula    # Aluno, Matricula, Modalidade (planos), politica (Strategy), portas
│   └── treino       # ficha (Iterator), geradores (Template Method), portas
├── application      # casos de uso + DTOs
├── infrastructure   # JPA (entities, repos, adapters), observers, beans, carga inicial
└── presentation     # controllers REST + tratador de erros
src/main/resources/static    # interface (index.html, estilo.css, app.js)
src/test/...                 # Cucumber: steps + features
```

### Endpoints

| Metodo | Rota | Descricao |
|---|---|---|
| GET | `/api/painel` | dashboard |
| GET | `/api/planos` | Plano Mensal e Plano Anual com valores |
| GET/POST | `/api/alunos` (`?busca=`) | listar/pesquisar e cadastrar |
| GET/PUT/DELETE | `/api/alunos/{id}` | detalhar, editar, excluir |
| GET/POST | `/api/matriculas` (`?busca=&plano=&status=&alunoId=`) | listar/filtrar e cadastrar |
| GET/PUT/DELETE | `/api/matriculas/{id}` | detalhar, editar (recalcula valor), excluir |
| POST | `/api/matriculas/{id}/pagamentos` | registrar pagamento |
| POST | `/api/matriculas/{id}/renovacoes` | renovar pelo mesmo plano |
| POST | `/api/matriculas/{id}/cancelamento` | cancelar |
| POST/GET | `/api/treinos/fichas?alunoId=` | gerar/listar ficha |
| POST | `/api/treinos/fichas/{id}/itens/{posicao}/conclusao` | concluir item |
| GET | `/api/treinos/fichas/{id}/proximo` | proximo exercicio |
| GET | `/api/eventos` | trilha de eventos |
| POST | `/api/ocorrencias/{id}/aguardando-peca` · `/retomada` · `/reabertura` | pausa/retoma o SLA, reabre |
| POST | `/api/matriculas/{id}/pagamentos/{indice}/estorno` | `{valor, categoria, motivo}` (valor vazio = saldo) |
| POST | `/api/contestacoes/{id}/pedido-de-informacoes` | equipe pergunta ao aluno |
| POST | `/api/area-aluno/contestacoes/{id}/resposta` · `/recurso` | aluno responde / recorre |
| POST | `/api/avaliacoes/{id}/resultado` · `/falta` | registra medidas (IMC) ou falta |
| POST | `/api/area-aluno/avaliacoes/{id}/recusa` | aluno recusa o horario proposto |
| POST | `/api/atendimentos/{id}/transferencia` | transfere para outro instrutor |
| POST | `/api/matriculas/{id}/trancamento` · `/destrancamento` | `{dias}` tranca / volta antes |
| POST | `/api/area-aluno/atendimentos/{id}/cancelamento` · `/avaliacao` | aluno cancela / avalia |

---

## Login e área do aluno

- Tela de login com **número da matrícula** e **senha**.
- **Aluno:** número da matrícula; senha de primeiro acesso = 6 primeiros dígitos do CPF (pode ser trocada na área do aluno). Na demo, a aluna Ana Lima já sai matriculada (o número aparece no log) com senha `111222`.
- **Equipe:** `EQ001` / `cesarfit`.
- Área do aluno: frequência com foguinho (check-in do dia), dados do aluno e da matrícula, treino da semana com conclusão de exercícios e troca de senha.
- A API exige o cabeçalho `X-Sessao` (token devolvido no login): `/api/area-aluno/**` só para ALUNO, o restante de `/api/**` só para EQUIPE.
- Cenários BDD em `src/test/resources/features/acesso.feature`.
