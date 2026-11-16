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

O prompt contém instruções fixas e apenas tema, tamanho, letras tentadas e posições reveladas. A palavra secreta fica no backend; não há memória de conversa nem ferramentas para a IA consultar o banco. O tema é uma dica fornecida pelo usuário: evite incluir a resposta nele. Palpites inválidos ou repetidos e falhas da API não consomem tentativas; solicite novamente. Os palpites são realmente produzidos pelo Gemini, sem substituição por sorteio local.

## Organização e tecnologias

- Java 17, Spring Boot 4.1.2 e Spring AI 2.0.1, com `ChatClient` e starter Google GenAI.
- `controller`: páginas e endpoint de palpite; `service`: regras e integração com Gemini.
- `model`: Partida, Jogada e ResultadoPartida; `repository`: Spring Data JPA; `dto`: estado público do jogo.
- Thymeleaf, CSS e JavaScript simples com SVG para o boneco.
- H2 em `data/forca.mv.db`, com partidas e jogadas persistentes. Hibernate cria/atualiza as tabelas. Para recomeçar com banco vazio, pare a aplicação e remova a pasta `data`.

Rotas: `GET /forca` (início/histórico), `POST /forca/iniciar`, `GET /forca/jogar/{id}` e `POST /forca/jogar/{id}/genai` (JSON). O estado retorna máscara, letras, erros, tentativas restantes e resultado; a palavra só é incluída no fim. Atualizações usam transação e versão otimista; em conflito, recarregue a página.

Este é um projeto acadêmico para execução local, sem autenticação ou isolamento de usuários. Não o exponha como serviço público com sua chave.

## Testar e empacotar

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
java -jar target/poo-spring-ai-0.0.1-SNAPSHOT.jar
```

Os testes usam H2 em memória e IA simulada, sem chamadas pagas ou necessidade de chave. Cobrem vitória, derrota, normalização, erros de API, respostas inválidas, prompt, páginas e persistência entre requisições. Execute o JAR a partir da raiz para carregar o `.env` e o mesmo banco.

O `.gitignore` exclui `.env`, banco local, builds, arquivos de IDE e ZIPs dos exemplos. Apenas `.env.example`, sem credenciais, é versionado.
