# CesarFit – Sistema de Gestão de Academia

Projeto da disciplina de Requisitos – CESAR School (trabalho em grupo, 6 integrantes).

**Stack:** Java 17 · Spring Boot 3.3 · Spring Data JPA/Hibernate · H2 (ou PostgreSQL) · Cucumber · HTML/CSS/JS.
**Abordagem:** Domain-Driven Design (níveis preliminar, estratégico, tático e operacional) e arquitetura limpa.

## Sumário

1. [Como executar](#1-como-executar)
2. [Novidades desta versão](#2-novidades-desta-versão)
3. [Requisitos do trabalho](#3-requisitos-do-trabalho)
4. [Funcionalidades e divisão pelos 6 integrantes](#4-funcionalidades-e-divisão-pelos-6-integrantes)
5. [Regras de negócio](#5-regras-de-negócio)
6. [Domain-Driven Design](#6-domain-driven-design)
7. [Padrões de projeto](#7-padrões-de-projeto)
8. [Arquitetura](#8-arquitetura)
9. [Interface web](#9-interface-web)
10. [API REST](#10-api-rest)
11. [Testes](#11-testes)
12. [Documentação do projeto](#12-documentação-do-projeto)

---

## 1. Como executar

### Pré-requisitos
- **JDK 17 ou superior** (`java -version`)
- **Maven 3.9+** (`mvn -v`)
- Opcional: VS Code com a extensão **Extension Pack for Java** (Microsoft)

### Rodando o sistema
1. Abra a pasta **`cesarfit`** (a que contém o `pom.xml`).
2. No terminal, dentro dessa pasta:
   ```bash
   mvn spring-boot:run
   ```
   No VS Code, também dá para abrir `src/main/java/school/cesar/gymflow/GymflowApplication.java` e clicar em **Run** acima do método `main`.
3. Acesse **http://localhost:8080**.

O banco H2 fica em memória: ao reiniciar, os dados voltam ao estado inicial (alunos de demonstração Ana Lima e Bruno Souza, catálogo de exercícios e uma ocorrência de equipamento).
Console do banco: http://localhost:8080/h2 (JDBC URL `jdbc:h2:mem:gymflow`, usuário `sa`, sem senha).

### Logins de demonstração
| Perfil | Número de acesso | Senha |
|---|---|---|
| Aluna (Ana Lima) | `1` (número da matrícula) | `111222` |
| Equipe | `EQ001` | `cesarfit` |

As senhas não são impressas no log do servidor.

### PostgreSQL (opcional)
Troque em `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gymflow
spring.datasource.username=postgres
spring.datasource.password=postgres
```

---

## 2. Novidades desta versão

Ajustes feitos em 18 e 19/11/2026. O histórico de uma versão anterior (remoção da catraca e dos planos antigos) está em [`cesarfit/ALTERACOES.md`](cesarfit/ALTERACOES.md).

### Regras de negócio
- **CPF:** recusa sequências repetidas (ex.: `111.111.111-11`); não pode ser alterado depois do cadastro (campo bloqueado na edição); a pesquisa por CPF é exata.
- **Exclusão de aluno:** quando o aluno é apagado, as presenças e a credencial de acesso também são removidas.
- **Ficha de treino:** ao gerar uma ficha nova, a anterior passa a **ARQUIVADA** (`StatusFicha`).
- **Segurança:** as senhas de demonstração não aparecem mais no log de inicialização.

### Interface
- **Número da matrícula** visível para a equipe na janela "Ver" do aluno.
- **Tabelas padronizadas:** coluna `#` em todas as listagens; dados sem quebra de linha; botões de ação sempre na mesma posição.
- **Cores dos botões:** Editar em laranja, Excluir e Estornar em vermelho, Cancelar em âmbar (também dentro das janelas).
- **Valores:** separador de milhar (`R$ 1.320,00`).
- **Datas:** sempre no formato `19/11/2026` (inclusive na trilha de eventos, que agora mostra o ano).
- **Acentuação:** nomes do catálogo, objetivos, grupos musculares e textos de eventos aparecem acentuados ("Tríceps", "Bicicleta ergométrica", "Média", "Matrícula"). A acentuação é só de exibição: os dados gravados não mudam, então cadastro, edição e exclusão funcionam como antes.

### Trilha de eventos
- Todas as categorias no mesmo formato (`MATRICULA_EFETIVADA`, `EQUIPAMENTO_INTERDITADO`, `DOR_RELATADA`).
- A matrícula aparece em uma linha só: "Aluno(a) Ana Lima, matrícula 1, matriculado(a) no Plano Mensal por R$ 120,00".

### DDD e arquitetura
- **Camada de aplicação em Java puro:** sem `@Service`, `@Component` ou `@Transactional`. Os casos de uso são registrados pela infraestrutura (`BeansDeAplicacao`).
- **Novo padrão Proxy:** `ProxyTransacional` aplica a transação nos métodos marcados com `@Transacional` (anotação da própria aplicação).
- **Contexto de Treino** não recebe mais a entidade `Aluno` de outro agregado, só o identificador e o objetivo.
- **Modelo estratégico (CML):** um só subdomínio principal (`Treinamento`); pagamentos e estornos documentados no agregado `Matricula`.

### Documentação
- Linguagem onipresente e mapa de histórias sem contradições (planos Mensal/Anual, check-in no lugar da catraca, "instrutor").
- **Protótipos recapturados** com o sistema atual.
- Divisão por integrante seguindo exatamente as combinações do enunciado (seção 4).

---

## 3. Requisitos do trabalho

| Requisito | Onde está |
|---|---|
| Java | Java 17 |
| Tecnologias sugeridas | Spring Boot e JPA (a interface é HTML/CSS/JS servida pelo Spring) |
| Descrição do domínio com linguagem onipresente | [`docs/linguagem-onipresente.md`](cesarfit/docs/linguagem-onipresente.md) |
| Mapa da história do usuário | [`docs/mapa-de-historias.md`](cesarfit/docs/mapa-de-historias.md) e [`mapa-de-historias.svg`](cesarfit/docs/mapa-de-historias.svg) |
| Protótipos de alta fidelidade | [`docs/prototipos.md`](cesarfit/docs/prototipos.md) (18 telas) |
| Modelo dos subdomínios no Context Mapper | [`docs/gymflow.cml`](cesarfit/docs/gymflow.cml) |
| Cenários de teste BDD | 141 cenários em `src/test/resources/features` |
| Automação BDD com Cucumber | `ExecutorDeCenarios` + classes de passos em `src/test/java/.../bdd` |
| Níveis preliminar, estratégico, tático e operacional do DDD | seção 6 |
| Arquitetura limpa | seção 8 |
| 4 ou mais padrões (Iterator, Decorator, Observer, Proxy, Strategy, Template Method) | 5 padrões: Strategy, Observer, Template Method, Iterator e Proxy (seção 7) |
| Persistência com mapeamento objeto-relacional | 14 entidades JPA em `infrastructure/persistence`, separadas do modelo de domínio |
| Camada de apresentação web | 11 controllers REST + interface em `src/main/resources/static` |
| Declaração de uso de IA | [`docs/declaracao-de-ia.md`](cesarfit/docs/declaracao-de-ia.md) |

---

## 4. Funcionalidades e divisão pelos 6 integrantes

**Regra do enunciado (por integrante):** 2 de complexidade alta **ou** 1 média + 2 baixas **ou** 4 baixas.
Cada integrante segue **exatamente uma** dessas combinações. Toda funcionalidade contada grava dados (nenhuma é só leitura). Listagens, filtros, indicadores do painel e a consulta do próximo exercício não são contados separadamente. As classificações são propostas da equipe e serão validadas pelo professor.

| Integrante | Combinação | Funcionalidade | Complexidade | Onde está no código | Cenários BDD |
|---|---|---|---|---|---|
| **1 – Alunos e operação** | 1 média + 2 baixas | Gerenciamento de alunos com responsável legal para menores, e-mail único, idade de 16 a 100 anos e exclusão com anonimização (LGPD) | Média | `CadastrarAlunoUseCase`, `AtualizarAlunoUseCase`, `ExcluirAlunoUseCase`, `AlunoController` | `alunos.feature` |
| | | Régua de avisos de vencimento (5 etapas, sem repetição nem regressão; a matrícula passa a VENCIDA) | Baixa | `EtapaDeAviso`, `NotificacaoDeVencimento`, `NotificacaoDeVencimentoUseCase`, `AgendadorDeVencimentos` | `operacao.feature` |
| | | Ocorrências de equipamentos com severidade, SLA, pausa por peça, interdição, reabertura e reincidência | Baixa | `OcorrenciaDeEquipamento`, `Severidade`, `OcorrenciaDeEquipamentoUseCase` | `operacao.feature` |
| **2 – Matrículas** | 2 altas | Efetivação de matrícula com preço por Strategy (Mensal/Anual), uma matrícula ativa por aluno e ficha gerada por evento (`MatriculaEfetivada`); inclui consultar e excluir | Alta | `MatricularAlunoUseCase`, `SeletorDePolitica`, `ExcluirMatriculaUseCase`, `MatriculaController` | `matriculas.feature` |
| | | Renovação, troca de plano com crédito pró-rata e trancamento do plano Anual | Alta | `RenovarMatriculaUseCase`, `AtualizarMatriculaUseCase`, `TrancamentoDeMatriculaUseCase` | `matriculas.feature` |
| | | *Adicional:* cancelamento com justificativa, data da solicitação e taxa de uma mensalidade se faltarem menos de 30 dias para o fim do contrato | – | `CancelarMatriculaUseCase`, `Matricula.cancelar` | `financeiro.feature` |
| **3 – Financeiro** | 1 média + 2 baixas | Pagamentos com multa e juros por atraso, troco só em dinheiro, parcelamento do Anual no crédito e renovação ao quitar atraso | Média | `Matricula.registrarPagamento`, `FormaDePagamento`, `RegistrarPagamentoUseCase` | `matriculas.feature` |
| | | Estorno total ou parcial com categoria, prazo de 90 dias, duplicidade e direito de arrependimento | Baixa | `Matricula.estornarPagamento`, `Pagamento`, `MotivoDeEstorno`, `EstornarPagamentoUseCase` | `financeiro.feature` |
| | | Contestação de cobrança com pedido de informações, resultado parcial, SLA, recurso único e limites | Baixa | `ContestacaoDeCobranca`, `StatusContestacao`, `ContestacaoDeCobrancaUseCase` | `financeiro.feature` |
| **4 – Fichas e catálogo** | 1 média + 2 baixas | Gerenciamento do catálogo de exercícios (incluir, editar, desativar, reativar, excluir) | Média | `Exercicio`, `CatalogoDeExerciciosUseCase`, `ExercicioController` | `catalogo.feature` |
| | | O aluno monta o próprio treino (inclui, ajusta e remove exercícios com séries e repetições) | Baixa | `PersonalizarFichaUseCase`, `FichaDeTreino.adicionarExercicio/ajustarItem/removerItem` | `personalizacao.feature` |
| | | Execução do treino: o aluno conclui cada exercício e o sistema registra o progresso | Baixa | `ConcluirItemDeTreinoUseCase`, `ConsultarProximoExercicioUseCase` | `treinos.feature` |
| | | *Adicional:* geração automática da ficha A/B/C por objetivo (Template Method), na matrícula, só com exercícios ativos e arquivando a anterior | – | `GerarFichaDeTreinoUseCase`, `GeradorDeFicha` e subclasses, `FabricaDeGerador` | `treinos.feature`, `personalizacao.feature` |
| **5 – Acesso e solicitações** | 1 média + 2 baixas | Autenticação com bloqueio após 5 senhas erradas, senha forte, sessão por inatividade e acesso limitado com matrícula vencida | Média | `EntrarNoSistemaUseCase`, `GerenciadorDeSessoes`, `FiltroDeSessao`, `AcessoController` | `acesso.feature` |
| | | Avaliação física: antecedência de 24h, agenda sem conflito, até 2 contrapropostas, resultado com IMC, faltas e bloqueio | Baixa | `SolicitacaoDeAvaliacao`, `ResultadoDeAvaliacao`, `AvaliacaoFisicaUseCase` | `solicitacoes.feature` |
| | | Substituição de exercício com categorias, limites por ficha, sugestões, aprovação automática por equipamento interditado e expiração | Baixa | `SolicitacaoDeSubstituicao`, `MotivoDeSubstituicao`, `SubstituicaoDeExercicioUseCase` | `solicitacoes.feature` |
| | | *Adicional:* alteração de senha | – | `AlterarSenhaUseCase` | `acesso.feature` |
| **6 – Frequência e treino** | 1 média + 2 baixas | Check-in com horário de funcionamento e bloqueio por inadimplência e por trancamento | Média | `RegistrarPresencaUseCase`, `Frequencia` | `acesso.feature` |
| | | Feedback pós-treino com carga (sRPE), ACWR, alertas e dor que aciona o instrutor | Baixa | `FeedbackDeTreino`, `AnaliseDeCarga`, `AlertaDeCarga`, `FeedbackPosTreinoUseCase` | `solicitacoes.feature` |
| | | Atendimento do instrutor com fila priorizada, SLA, limite por instrutor, transferência, cancelamento e nota | Baixa | `SolicitacaoDeAtendimento`, `TipoDeAtendimento`, `AtendimentoDoInstrutorUseCase` | `solicitacoes.feature` |

**Total:** 1 integrante × 2 altas + 5 integrantes × (1 média + 2 baixas) = **17 funcionalidades contadas**.
Cancelamento de matrícula, alteração de senha e geração automática da ficha são entregas adicionais (não contadas).

---

## 5. Regras de negócio

### 5.1 Regras gerais
- Existem **somente dois planos**: **Plano Mensal = R$ 120,00** (vigência de 1 mês) e **Plano Anual = R$ 1.320,00** (vigência de 12 meses).
- O valor **nunca vem da tela**: o servidor calcula pelo plano (Strategy) no cadastro e na edição e grava na matrícula.
- Um aluno só pode ter **uma matrícula ativa** por vez.
- Aluno com matrícula ativa **não pode ser excluído**.
- Validações com mensagens claras: CPF (11 dígitos, único, sem sequência repetida), telefone (DDD + número), e-mail, idade (16 a 100 anos), aluno e plano obrigatórios na matrícula, data de início obrigatória.
- Toda regra com prazo recebe o "agora" pela porta `domain.shared.Relogio`, o que torna o domínio determinístico e testável.

### 5.2 Funcionalidades médias e altas

**Integrante 1 – Gerenciamento de alunos (média)**
| Regra | Detalhe |
|---|---|
| Responsável legal | aluno de 16 ou 17 anos exige nome e CPF do responsável (CPF diferente do aluno) |
| Unicidade | CPF e e-mail únicos entre os alunos |
| CPF imutável | o CPF não muda depois do cadastro |
| Idade | de 16 a 100 anos |
| Exclusão (LGPD) | só sem matrícula ativa. Sem histórico financeiro, o aluno é apagado junto com matrículas, ficha, presenças e credencial. Com pagamentos, ele é **anonimizado**: o histórico financeiro fica e o aluno some das listagens |

**Integrante 2 – Renovação, troca de plano e trancamento (alta)**
| Regra | Detalhe |
|---|---|
| Troca de plano pró-rata | o valor pago e não usado (por dia) vira crédito abatido do novo plano |
| Trancamento | só plano Anual, ativo, vigente e em dia; mínimo de 7 dias; até 60 dias e 2 trancamentos por contrato; estende a vigência e bloqueia o check-in |
| Destrancar antes | os dias não usados voltam ao saldo e saem da vigência |

**Integrante 3 – Pagamentos (média)**
| Regra | Detalhe |
|---|---|
| Valor devido | plano − crédito de troca + multa de 2% + juros de 0,033% ao dia quando pago depois do fim da vigência |
| Formas | PIX, dinheiro, débito, crédito e boleto; só dinheiro aceita valor maior (troco); as demais exigem o valor exato |
| Parcelamento | só cartão de crédito e só plano Anual, até 12x |
| Atraso quitado | o pagamento em atraso renova a vigência a partir do vencimento anterior |
| Inadimplência | 5 dias após o início sem pagamento: matrícula com pagamento pendente (bloqueia check-in e trancamento) |

**Integrante 4 – Catálogo de exercícios (média)**
| Regra | Detalhe |
|---|---|
| Duplicidade | não existem dois exercícios com o mesmo nome |
| Uso em fichas | exercício usado em alguma ficha não pode ser excluído nem mudar de nome ou de grupo; só pode ser desativado |
| Último ativo do grupo | não é possível desativar o último exercício ativo de um grupo muscular |
| Ativos nas fichas | só exercícios ativos entram em fichas novas e nas substituições |

**Integrante 5 – Autenticação e sessão (média)**
| Regra | Detalhe |
|---|---|
| Força bruta | 5 senhas erradas seguidas bloqueiam o número de acesso por 15 minutos |
| Senha forte | mínimo de 8 caracteres, com letras e números, sem conter os dígitos do CPF |
| Primeiro acesso | o login avisa que a senha inicial (6 primeiros dígitos do CPF) deve ser trocada |
| Sessão | expira com 30 minutos sem uso ou 8 horas após o login |
| Matrícula vencida | o aluno entra por até 30 dias depois do vencimento (para renovar); depois disso, não |

**Integrante 6 – Check-in (média)**
| Regra | Detalhe |
|---|---|
| Horário | das 6h às 22h; um check-in por dia; não aceita data futura |
| Bloqueios | matrícula vigente, não trancada e sem pagamento pendente |

### 5.3 Funcionalidades baixas

As funcionalidades baixas também têm regras com prazos, cálculos, limites e integração entre contextos, todas com cenários BDD.

**Integrante 1 – Régua de avisos de vencimento**
| Regra | Detalhe |
|---|---|
| 5 etapas | Lembrete (7 a 4 dias antes), Vence em breve (3 a 1 dia), Vence hoje, Matrícula vencida (1 a 4 dias depois) e Último aviso (5 a 15 dias depois) |
| Sem repetição | cada etapa é enviada uma vez por matrícula e vencimento (restrição única `matricula_id, vencimento, etapa`) |
| Sem regressão | se uma etapa posterior já foi enviada, uma anterior nunca é enviada |
| Verificação atrasada | envia só a etapa do dia, sem "despejar" as perdidas |
| Cobrança inteligente | as etapas de cobrança não são enviadas se o aluno já tem outra matrícula vigente; matrícula cancelada não recebe aviso |
| Efeito na matrícula | ao entrar na fase de cobrança, a matrícula passa para VENCIDA |
| Limite | no máximo 1 aviso por aluno por verificação |

**Integrante 1 – Ocorrências de equipamentos**
| Regra | Detalhe |
|---|---|
| Severidade e SLA | Baixa 72h, Média 48h, Alta 24h, Crítica 4h; fora do prazo, aparece como atrasada |
| Interdição | Alta e Crítica interditam o equipamento até a resolução (usado pela substituição de exercício) |
| Pausa do SLA | Aberta → Em andamento ⇄ Aguardando peça: o tempo esperando peça não conta no prazo |
| Custo | se houve troca de peça, o custo do reparo é obrigatório na resolução |
| Reabertura | só até 7 dias após resolvida e no máximo 2 vezes; o SLA recomeça |
| Reincidência | 2 ocorrências resolvidas do mesmo equipamento em 30 dias: a próxima nasce reincidente e com severidade um nível acima |
| Fila | pendentes primeiro, depois atrasadas, mais graves e prazo mais próximo |

**Integrante 3 – Estorno de pagamento**
| Regra | Detalhe |
|---|---|
| Parcial ou total | vários estornos parciais até zerar o saldo; o total pago considera só o saldo não estornado |
| Prazo | até 90 dias da data do pagamento (exceto quando vem de contestação procedente) |
| Categorias | `COBRANCA_DUPLICADA`, `VALOR_INCORRETO`, `DESISTENCIA`, `CONTESTACAO_PROCEDENTE`, `OUTRO` |
| Duplicidade | exige outro pagamento ativo de mesmo valor em até 3 dias e estorno integral |
| Arrependimento | desistência só em até 7 dias (CDC, art. 49), integral, e cancela a matrícula **sem taxa** |

**Integrante 3 – Contestação de cobrança**
| Regra | Detalhe |
|---|---|
| Prazo para contestar | até 30 dias do pagamento; só uma contestação em aberto por pagamento |
| Pedido de informações | a equipe pode perguntar ao aluno no máximo 2 vezes; aguardando resposta, não pode decidir |
| Resultados | procedente (estorna o saldo), parcialmente procedente (estorna o valor informado) ou improcedente |
| SLA | análise em até 5 dias (da abertura ou do recurso); depois disso, aparece atrasada |
| Recurso | uma única vez, até 7 dias após a decisão, julgado por **outro analista** |
| Limite | 2 contestações improcedentes em 180 dias bloqueiam novas contestações |

**Integrante 5 – Avaliação física**
| Regra | Detalhe |
|---|---|
| Horário | futuro, com pelo menos 24h de antecedência, até 60 dias, das 6h às 21h, em horários cheios ou meia hora |
| Agenda | um aluno por horário (confirmado ou proposto) |
| Contrapropostas | o aluno pode recusar o horário proposto; a equipe propõe no máximo 2 vezes |
| Resultado | só depois do horário; calcula IMC e classificação (OMS) no objeto de valor `ResultadoDeAvaliacao` |
| Faltas | falta só após 15 min de tolerância; 2 faltas em 90 dias bloqueiam agendamentos por 30 dias |
| Intervalo | nova avaliação só 30 dias após a última; cancelamento de avaliação confirmada só até 2h antes |

**Integrante 5 – Substituição de exercício**
| Regra | Detalhe |
|---|---|
| Categorias | dor ou lesão (prioridade máxima), equipamento indisponível, dificuldade técnica, preferência |
| Limites | 3 trocas aprovadas por ficha; preferência só 1 por ficha |
| Sugestões | substitutos do mesmo grupo, ativos, fora da ficha, sem equipamento interditado (equipamento diferente primeiro) |
| Aprovação automática | equipamento indisponível com o equipamento interditado por ocorrência é aprovado na hora com a melhor sugestão |
| Expiração e fila | pedido pendente expira em 7 dias; fila da equipe ordenada por prioridade e antiguidade |

**Integrante 6 – Feedback pós-treino**
| Regra | Detalhe |
|---|---|
| Dados | esforço de 1 a 10, duração de 10 a 240 min, dor (com local obrigatório) e observação; exige check-in e ao menos um exercício concluído |
| Carga | sRPE = esforço × duração |
| ACWR | carga aguda (7 dias) ÷ carga crônica (média semanal de 28 dias), com pelo menos 4 registros; acima de 1,5 = risco de lesão |
| Tendência | 3 treinos seguidos com esforço ≥ 9 = sobrecarga; 3 treinos com esforço ≤ 3 e ficha completa = progressão sugerida |
| Dor | gera o alerta de dor e **abre automaticamente** um atendimento de prioridade alta para o instrutor |

**Integrante 6 – Atendimento do instrutor**
| Regra | Detalhe |
|---|---|
| Prioridade | dor ou lesão = alta; demais = normal; fila por prioridade e depois por ordem de chegada (posição exibida ao aluno) |
| Ordem | não é possível assumir um pedido normal enquanto houver um de prioridade alta aguardando |
| SLA | primeira resposta em 10 min (alta) ou 30 min (normal) |
| Capacidade | cada instrutor atende no máximo 3 alunos ao mesmo tempo (vale para transferências) |
| Transferência | para outro instrutor, no máximo 2 vezes |
| Aluno | cancela enquanto não foi assumido; avalia de 1 a 5 uma única vez, em até 48h, com comentário obrigatório para nota 1 ou 2 |

---

## 6. Domain-Driven Design

| Nível | O que o projeto tem | Onde |
|---|---|---|
| **Preliminar** | descrição do domínio, glossário e regras na linguagem onipresente, usada igual na conversa, no código e nos testes | [`linguagem-onipresente.md`](cesarfit/docs/linguagem-onipresente.md) |
| **Estratégico** | 6 subdomínios (1 principal, 4 de suporte, 1 genérico), 6 contextos delimitados e o mapa de contexto com os tipos de relação | [`gymflow.cml`](cesarfit/docs/gymflow.cml) |
| **Tático** | entidades, objetos de valor imutáveis (`Cpf`, `Dinheiro`, `Texto`, `Pagamento`), agregados ligados por identificador, repositórios como interfaces, serviços de domínio, fábricas, eventos e módulos por contexto | `src/main/java/.../domain` |
| **Operacional** | camadas de domínio, aplicação, infraestrutura e apresentação; modelo de domínio separado do modelo de persistência | seção 8 |

### Subdomínios e contextos
| Subdomínio | Tipo | Contexto delimitado | Pacote |
|---|---|---|---|
| Treinamento | **Principal** | `TreinoContext` | `domain.treino` |
| Gestão de matrículas (inclui pagamentos e estornos) | Suporte | `MatriculaContext` | `domain.matricula` |
| Financeiro (contestações) | Suporte | `FinanceiroContext` | `domain.financeiro` |
| Atendimento ao aluno | Suporte | `AtendimentoContext` | `domain.atendimento` |
| Operação da academia | Suporte | `OperacaoContext` | `domain.operacao` |
| Controle de acesso | Genérico | `AcessoContext` | `domain.acesso` |

### Eventos de domínio entre contextos
Os contextos não chamam uns aos outros: o agregado registra um **evento tipado** (`domain.shared.evento.Evento`), o caso de uso o publica no `BarramentoDeEventos` e o contexto interessado reage com um `ManipuladorDeEvento` (pacote `application.reacao`). O fato também fica na trilha de eventos (aba Eventos).

| Evento | Publicado por | Quem reage | Reação |
|---|---|---|---|
| `MatriculaEfetivada` | Matrícula | Treino (`GerarFichaAoEfetivarMatricula`) | gera a ficha A/B/C |
| `DorRelatada` | Treino (feedback) | Atendimento (`AbrirAtendimentoAoRelatarDor`) | abre atendimento prioritário |
| `EquipamentoInterditado` / `EquipamentoLiberado` | Operação (ocorrência) | Atendimento (`MarcarEquipamentoIndisponivel`, `LiberarEquipamento`) | atualiza os equipamentos indisponíveis, usados na troca de exercício |
| `ContestacaoJulgadaProcedente` | Financeiro (contestação) | Matrícula (`EstornarAoJulgarContestacao`) | estorna o pagamento |

---

## 7. Padrões de projeto

| Padrão | Implementação | Problema que resolve |
|---|---|---|
| **Strategy** | `PoliticaDePreco` com `PrecoMensal` e `PrecoAnual`, escolhidos por `SeletorDePolitica` | calcular o valor do plano em um único lugar, sem `if` espalhado |
| **Observer** | `PublicadorDeEventos` com `NotificadorPorEmail` e `TrilhaDeAuditoria` | reagir a eventos de domínio sem acoplar os casos de uso |
| **Template Method** | `GeradorDeFicha` com `GeradorHipertrofia`, `GeradorEmagrecimento` e `GeradorCondicionamento` | fixar o esqueleto da geração da ficha, variando as vagas de cada objetivo |
| **Iterator** | `FichaDeTreino.iterator()` / `IteradorDePendentes` | percorrer só os itens pendentes sem expor a coleção |
| **Proxy** | `ProxyTransacional`, aplicado por `BeansDeAplicacao` | controlar o acesso aos casos de uso abrindo, confirmando ou desfazendo a transação, sem o caso de uso conhecer o Spring |
| Factory Method (extra) | `FabricaDeGerador.para(objetivo)` | escolher o gerador de ficha pelo objetivo |

---

## 8. Arquitetura

```
presentation  ->  application  ->  domain  <-  infrastructure
   (REST)        (casos de uso)    (regras)    (JPA, eventos, montagem)
```

- **Domínio e aplicação não dependem de framework:** nenhuma classe dessas camadas importa Spring ou JPA.
- **Inversão de dependência:** os repositórios são interfaces no domínio, implementadas por adaptadores JPA na infraestrutura.
- **Dois modelos:** o modelo de domínio é convertido no modelo de persistência (`*Entity`) pelos adaptadores, e o JPA mapeia o modelo de persistência para as tabelas.
- **Montagem na infraestrutura:** `BeansDeAplicacao` registra os casos de uso e aplica o `ProxyTransacional` nos métodos marcados com `@Transacional`; `BeansDeDominio` monta o Observer e o Strategy.
- **Consultas separadas dos comandos:** os casos de uso alteram dados; `ServicoDeConsultas` só lê.

```
src/main/java/school/cesar/gymflow
├── domain           # regras de negócio puras (sem framework)
│   ├── shared       # Dinheiro, Cpf, Texto, Relogio, eventos, Observer
│   ├── matricula    # Aluno, Matricula, Pagamento, Modalidade, política de preço (Strategy), avisos
│   ├── financeiro   # ContestacaoDeCobranca
│   ├── treino       # Exercicio, FichaDeTreino (Iterator), geradores (Template Method), feedback
│   ├── atendimento  # avaliação física, substituição de exercício, atendimento do instrutor
│   ├── operacao     # ocorrências de equipamentos
│   └── acesso       # credenciais e senha
├── application      # casos de uso, reações a eventos, DTOs e a anotação @Transacional (Java puro)
├── infrastructure   # persistência JPA, barramento de eventos, observers, agendador,
│                    # BeansDeAplicacao, BeansDeDominio e ProxyTransacional
└── presentation     # controllers REST, filtro de sessão e tratador de erros
src/main/resources/static   # interface web (index.html, estilo.css, acesso.css, app.js...)
src/test                    # cenários BDD (Cucumber) e testes de unidade
```

---

## 9. Interface web

- **Login** com número de acesso e senha. O aluno entra com o número da matrícula (senha inicial = 6 primeiros dígitos do CPF); a equipe entra com o número de acesso (ex.: `EQ001`).
- **Equipe:** Painel, Alunos e matrículas, Treinos, Exercícios, Solicitações, Equipamentos e Eventos.
- **Área do aluno:** frequência com sequência de dias (check-in), dados da matrícula, treino da semana (concluir, trocar e montar exercícios), feedback pós-treino, solicitações, financeiro e troca de senha.
- **Padrões visuais:** mesmo layout de tabela em todas as telas; Editar em laranja, Excluir em vermelho e Cancelar em âmbar; valores como `R$ 1.320,00`; datas como `19/11/2026`; textos acentuados.
- A API exige o cabeçalho `X-Sessao` (token devolvido no login): `/api/area-aluno/**` só para ALUNO; o restante de `/api/**` só para EQUIPE.

Telas: veja os [protótipos](cesarfit/docs/prototipos.md).

---

## 10. API REST

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/acesso/login` · `/logout` | entrar e sair |
| GET | `/api/painel` · `/api/planos` · `/api/eventos` | painel, planos e trilha de eventos |
| GET/POST | `/api/alunos` (`?busca=`) | listar, pesquisar e cadastrar |
| GET/PUT/DELETE | `/api/alunos/{id}` | detalhar, editar e excluir |
| GET/POST | `/api/matriculas` (`?busca=&plano=&status=&alunoId=`) | listar, filtrar e matricular |
| GET/PUT/DELETE | `/api/matriculas/{id}` | detalhar, editar (recalcula o valor) e excluir |
| POST | `/api/matriculas/{id}/pagamentos` · `/renovacoes` · `/cancelamento` | pagar, renovar e cancelar |
| POST | `/api/matriculas/{id}/trancamento` · `/destrancamento` | trancar (`{dias}`) e destrancar |
| POST | `/api/matriculas/{id}/pagamentos/{indice}/estorno` | estornar (`{valor, categoria, motivo}`; valor vazio = saldo) |
| GET/POST | `/api/exercicios` | listar e incluir no catálogo |
| PUT/DELETE | `/api/exercicios/{id}` | editar e excluir |
| POST | `/api/exercicios/{id}/desativacao` · `/reativacao` | desativar e reativar |
| GET/POST | `/api/treinos/fichas?alunoId=` | consultar e gerar a ficha |
| GET/POST | `/api/notificacoes` · `/api/notificacoes/verificacao` | avisos de vencimento |
| GET/POST | `/api/ocorrencias` | listar e registrar ocorrências |
| POST | `/api/ocorrencias/{id}/andamento` · `/aguardando-peca` · `/retomada` · `/resolucao` · `/reabertura` | fluxo da ocorrência |
| GET/POST | `/api/contestacoes` · `/{id}/analise` · `/{id}/pedido-de-informacoes` | contestações (equipe) |
| GET/POST | `/api/avaliacoes` · `/{id}/confirmacao` · `/proposta` · `/resultado` · `/falta` | avaliações físicas (equipe) |
| GET/POST | `/api/substituicoes` · `/{id}/aprovacao` · `/rejeicao` | substituições (equipe) |
| GET/POST | `/api/atendimentos` · `/{id}/assuncao` · `/transferencia` · `/conclusao` | atendimentos (equipe) |
| GET | `/api/area-aluno` | dados da área do aluno |
| POST | `/api/area-aluno/presencas` | check-in |
| POST | `/api/area-aluno/fichas/{fichaId}/itens/{posicao}/conclusao` | concluir exercício |
| POST/PUT/DELETE | `/api/area-aluno/ficha/itens` · `/{posicao}` | montar o próprio treino |
| POST | `/api/area-aluno/feedbacks` · `/avaliacoes` · `/substituicoes` · `/atendimentos` · `/contestacoes` | pedidos do aluno |
| PUT | `/api/area-aluno/senha` | trocar a senha |

---

## 11. Testes

```bash
mvn test        # relatório do Cucumber em target/cucumber-report.html
```

- **185 testes:** 141 cenários BDD e 44 testes de unidade, todos passando.
- **BDD:** cenários em `src/test/resources/features` (alunos, matrículas, catálogo, personalização, treinos, acesso, operação, financeiro e solicitações), automatizados com Cucumber. Cada cenário começa com o banco limpo (só o catálogo padrão de exercícios).
- **Unidade:** `CpfTest`, `DinheiroTest`, `TextoTest`, `MatriculaTest`, `PagamentoTest`, `FichaDeTreinoTest` e `ProxyTransacionalTest`.
- **Tempo controlável:** o relógio dos testes (`RelogioDeTeste`) permite testar prazos e SLAs avançando o tempo.

---

## 12. Documentação do projeto

| Documento | Conteúdo |
|---|---|
| [`linguagem-onipresente.md`](cesarfit/docs/linguagem-onipresente.md) | descrição do domínio, glossário e regras principais |
| [`mapa-de-historias.md`](cesarfit/docs/mapa-de-historias.md) | mapa da história do usuário (quadro e tabelas) |
| [`prototipos.md`](cesarfit/docs/prototipos.md) | protótipos de alta fidelidade (18 telas) |
| [`gymflow.cml`](cesarfit/docs/gymflow.cml) | modelo estratégico e tático no Context Mapper |
| [`declaracao-de-ia.md`](cesarfit/docs/declaracao-de-ia.md) | declaração de uso de IA |
| [`interacoes-ia/`](cesarfit/docs/interacoes-ia/) | registro das interações com IA |
| [`ALTERACOES.md`](cesarfit/ALTERACOES.md) | histórico de uma versão anterior |
