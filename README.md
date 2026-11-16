# Forca com Spring AI

Trabalho de Programação Orientada a Objetos — Univille.

**Autores:** Andrey Rebelatto, Eduardo Will e Erick Anderson.

Sistema Web em Java no qual o usuário escolhe um tema e uma palavra secreta, e o Gemini tenta descobri-la. A estrutura segue os exemplos da disciplina: controllers MVC, serviços, entidades JPA, repositórios e templates Thymeleaf.

## Executar

Requisitos: JDK 17 ou superior e acesso à internet. O Maven Wrapper baixa o Maven e as dependências no primeiro uso.

1. Copie `.env.example` para `.env` na raiz do projeto.
2. Preencha `GEMINI_API_KEY` com sua chave do Google AI Studio. Opcionalmente altere `GEMINI_MODEL` conforme os modelos disponíveis para sua conta.
3. Execute no Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux/macOS:

```sh
sh mvnw spring-boot:run
```

Abra http://localhost:8080. O `.env` é carregado automaticamente como arquivo de propriedades. Use `CHAVE=valor`, sem `export` ou aspas. Variáveis do sistema têm precedência. A aplicação pode abrir sem chave, mas os palpites exigem uma chave válida e cota disponível.

## Como jogar

Informe um tema e uma palavra de 2 a 30 letras. Não use espaços, números ou símbolos. Acentos e cedilha são normalizados: `ação` vira `ACAO`.

Clique em **Pedir palpite da IA** para cada letra. Todas as ocorrências de uma letra correta são reveladas. A IA vence quando revela a palavra e perde no quinto erro. O boneco perde perna direita, perna esquerda, braço direito, braço esquerdo e, no último erro, cabeça e tronco. O histórico permite consultar ou continuar partidas.

O prompt contém instruções fixas e apenas tema, tamanho, letras tentadas e posições reveladas. A palavra secreta fica no backend; não há memória de conversa nem ferramentas para a IA consultar o banco. O tema é uma dica fornecida pelo usuário: evite incluir a resposta nele. Palpites inválidos ou repetidos e falhas da API não consomem tentativas. Os palpites são realmente produzidos pelo Gemini, sem substituição por sorteio local.

A tela mostra um registro em tempo real com horário: início de cada chamada, falhas, novas tentativas, letra recebida e confirmação da jogada. Há uma chamada inicial e até três novas tentativas consecutivas em caso de falha (quatro chamadas no máximo), com timeout de 20 segundos por chamada. As tentativas internas do SDK são desabilitadas para manter esse limite. O contador indica o tempo decorrido, sem simular uma resposta da IA. Se a conexão cair ou o tempo máximo da página expirar, recarregue para conferir se a jogada foi registrada antes de pedir outro palpite.

## Organização e tecnologias

- Java 17, Spring Boot 4.1.2 e Spring AI 2.0.1, com `ChatClient` e starter Google GenAI.
- `controller`: páginas e endpoint de palpite; `service`: regras e integração com Gemini.
- `model`: Partida, Jogada e ResultadoPartida; `repository`: Spring Data JPA; `dto`: estado público do jogo.
- Thymeleaf, CSS e JavaScript simples com SVG para o boneco.
- H2 em `data/forca.mv.db`, com partidas e jogadas persistentes. Hibernate cria/atualiza as tabelas. Para recomeçar com banco vazio, pare a aplicação e remova a pasta `data`.

Rotas: `GET /forca` (início/histórico), `POST /forca/iniciar`, `GET /forca/jogar/{id}` e `POST /forca/jogar/{id}/genai` (JSON). A interface usa `POST /forca/jogar/{id}/genai/acompanhar`, que transmite eventos SSE `progresso`, `resultado` ou `falha`. O registro contém mensagens operacionais e a letra validada, sem expor credenciais, palavra secreta ou raciocínio interno do modelo. O estado retorna máscara, letras, erros, tentativas restantes e resultado; a palavra só é incluída no fim. Atualizações usam transação e versão otimista; em conflito, recarregue a página.

Este é um projeto acadêmico para execução local, sem autenticação ou isolamento de usuários. Não o exponha como serviço público com sua chave.

## Testar e empacotar

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
java -jar target/poo-spring-ai-0.0.1-SNAPSHOT.jar
```

Os testes usam H2 em memória e IA simulada, sem chamadas pagas ou necessidade de chave. Cobrem vitória, derrota, normalização, erros de API, respostas inválidas, prompt, páginas e persistência entre requisições. Execute o JAR a partir da raiz para carregar o `.env` e o mesmo banco.

O `.gitignore` exclui `.env`, banco local, builds, arquivos de IDE e ZIPs dos exemplos. Apenas `.env.example`, sem credenciais, é versionado.

## Validação da entrega

Na entrega inicial, os sete testes automatizados passaram e o JAR foi gerado. As páginas foram verificadas por testes MVC e por requisições HTTP locais. O banco em arquivo foi reaberto após reiniciar a aplicação, preservando a partida. A suíte também cobre o limite de novas tentativas e os eventos de progresso, sucesso e falha.

A chamada real ao Gemini não pôde ser concluída neste ambiente: o modelo `gemini-2.5-flash` retornou HTTP 404 informando indisponibilidade para novos usuários e recomendando `gemini-3.8-flash`. Esse passou a ser o padrão configurado; a verificação seguinte terminou em timeout. Portanto, a integração real ainda precisa ser confirmada em um ambiente com acesso ao modelo e cota disponível. O jogo trata essa falha sem consumir tentativas. Não foi feita inspeção visual em navegador nesta validação.
