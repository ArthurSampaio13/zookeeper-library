### Visão Geral do Apache Zookeeper

O Apache Zookeeper é um projeto de software da Apache Software Foundation. Ele oferece um serviço para sistemas distribuídos, fornecendo uma loja de chave-valor hierárquica. Essa tecnologia é utilizada para oferecer serviços distribuídos de configuração, sincronização e registro de nomes, essenciais para grandes sistemas distribuídos.

O Zookeeper é, essencialmente, uma biblioteca que facilita a coordenação em sistemas distribuídos. Abaixo estão alguns dos problemas típicos de coordenação em sistemas distribuídos que o Zookeeper resolve:

1. **Gerenciamento de Configuração** — Consiste em gerenciar configurações de aplicativos que podem ser compartilhadas entre servidores em um cluster. A ideia é manter todas as configurações em um local centralizado, garantindo que todos os servidores sejam notificados e atualizados sempre que houver mudanças nos arquivos ou dados de configuração.

2. **Eleição de Líder** — Em um cluster com vários nós, pode ser necessário eleger um líder, por exemplo, para centralizar a recepção de requisições de atualização ou distribuir tarefas entre os nós trabalhadores. O Zookeeper facilita esse processo de forma eficiente.

3. **Locks em Sistemas Distribuídos** — Permite que diferentes sistemas adquiram um "lock" para operar em um recurso compartilhado de forma mutuamente exclusiva. Imagine um cenário onde diversos servidores precisam escrever em um arquivo ou em qualquer outro dado compartilhado. Antes de realizar a atualização, cada servidor adquire um lock e o libera após a operação.

4. **Gerenciamento de Membros do Cluster** — O Zookeeper pode manter o controle e detectar quando um servidor entra ou sai de um cluster, além de armazenar outras informações complexas sobre a composição do cluster.

### Zookeeper ZNodes

O Zookeeper resolve esses problemas utilizando uma estrutura em árvore chamada **znodes**, que se assemelha ao sistema de arquivos Unix. Esses znodes são análogos a diretórios e arquivos no sistema de arquivos, mas com funcionalidades adicionais.

![struct](https://github.com/rgederin/zookeeper-integration/blob/master/img/struct.jpg)

O Zookeeper oferece operações primitivas para manipular esses znodes, o que nos permite resolver os desafios de sistemas distribuídos.

### Zookeeper operations
![ops](https://github.com/rgederin/zookeeper-integration/blob/master/img/ops.png)

### Principais Características dos Znodes

- Znodes podem armazenar dados e ter znodes filhos simultaneamente.
- Eles podem armazenar informações como a versão atual das alterações de dados no znode e o ID de transação da última operação realizada.
- Cada znode pode ter sua própria lista de controle de acesso (ACL), semelhante às permissões em sistemas Unix. O Zookeeper oferece permissões para criar, ler, escrever, excluir e administrar (definir/editar permissões) znodes.
- O ACL dos znodes suporta autenticação baseada em nome de usuário e senha para cada znode individualmente.
- Os clientes podem definir **watchers** em znodes para serem notificados quando houver alterações nos mesmos, como modificações nos dados, mudanças nos znodes filhos, criação de novos filhos ou exclusão de um filho.

### Tipos de ZNodes e seus Casos de Uso

Existem quatro tipos de Znodes no Zookeeper:

1. **Znodes Persistentes**
2. **Znodes Efêmeros**
3. **Znodes Efêmeros Sequenciais**
4. **Znodes Persistentes Sequenciais**

#### 1. Znodes Persistentes
Como o nome sugere, uma vez criados, esses znodes permanecem no Zookeeper até serem removidos manualmente (usando a operação de exclusão). Esse tipo de znode é ideal para armazenar informações de configuração ou dados que precisam ser persistentes. Todos os servidores do cluster podem acessar e consumir os dados de um znode persistente.

#### 2. Znodes Efêmeros
Esses znodes são automaticamente excluídos pelo Zookeeper quando o cliente que os criou termina sua sessão com o Zookeeper. Os clientes mantêm sua sessão ativa enviando periodicamente requisições de ping. Se o Zookeeper não receber esses pings dentro do tempo configurado de timeout da sessão, ele considera que o cliente morreu e exclui tanto a sessão quanto os znodes criados por esse cliente.

O uso típico desse tipo de znode é para manter uma lista de servidores ativos em um cluster. Por exemplo, podemos criar um znode pai chamado "/servidores_ativos", onde cada novo servidor no cluster cria um znode filho. Se um servidor falhar, o znode filho correspondente será excluído, e outros servidores, que estejam observando o znode "/servidores_ativos", serão notificados dessa mudança.

#### 3. Znodes Efêmeros Sequenciais
Semelhante aos znodes efêmeros, mas com uma diferença: o Zookeeper anexa um número sequencial como sufixo ao nome do znode. Se um novo znode irmão do mesmo tipo for criado, ele receberá um número maior que o anterior.

Esse tipo de znode pode ser utilizado, por exemplo, em algoritmos de eleição de líder. Suponha que temos um znode pai chamado "/eleicao" e que, para cada novo nó que entra no cluster, criamos um znode efêmero sequencial dentro de "/eleicao". O servidor que criar o znode com o menor número sequencial será considerado o líder. Se esse líder cair, o Zookeeper excluirá o znode correspondente e os clientes serão notificados para eleger um novo líder, baseado no menor número sequencial restante.

#### 4. Znodes Persistentes Sequenciais
Trata-se de um znode persistente com um número sequencial anexado ao seu nome como sufixo. Este tipo de znode é raramente utilizado, seguindo o mesmo princípio dos znodes efêmeros sequenciais, mas sem ser excluído automaticamente.

### Receitas do Apache Zookeeper

Abaixo estão algumas das receitas comuns usadas no Zookeeper para resolver problemas de sistemas distribuídos.

#### Eleição de Líder

##### Abordagem 1: Usando um único znode efêmero /leader
1. Um cliente (qualquer servidor pertencente ao cluster) cria um znode persistente `/election` no Zookeeper.
2. Todos os clientes adicionam uma **watch** ao znode `/election` para monitorar qualquer adição ou exclusão de znodes filhos.
3. Cada servidor que ingressa no cluster tenta criar um znode efêmero `/leader` dentro do znode `/election` com o nome do host como dados, por exemplo, `node1.dominio.com`. Como vários servidores tentarão criar znodes com o mesmo nome (`/leader`), apenas um terá sucesso, sendo considerado o líder.
4. Quando o líder cai, o Zookeeper exclui automaticamente o znode `/leader` após o tempo limite da sessão, notificando todos os servidores que monitoravam `/election`. Os servidores tentarão novamente criar o znode `/leader`, e apenas um será bem-sucedido, tornando-se o novo líder.

Essa abordagem pode criar um **efeito de manada**, onde todos os servidores tentam simultaneamente se tornar o líder, sobrecarregando o sistema.

##### Abordagem 2: Usando Znodes Efêmeros Sequenciais
1. Cada servidor no cluster tenta criar um znode sequencial efêmero sob o znode `/election`, por exemplo, `/election/leader-00000001`, `/election/leader-00000002`, etc.
2. O znode com o menor número sequencial é considerado o líder. Os servidores podem obter o nome do líder usando o comando `getChildren("/election")`.
3. Quando o líder atual cai, o Zookeeper exclui o znode correspondente e notifica os servidores. O servidor com o segundo menor número sequencial assume como líder.

Essa abordagem reduz o tráfego de rede, pois os servidores não precisam enviar uma solicitação de escrita ao Zookeeper para se tornarem líderes, mas ainda pode ocorrer um efeito de manada se muitos servidores forem notificados ao mesmo tempo.

##### Abordagem 3: Usando Znodes Efêmeros Sequenciais, mas notificando apenas um servidor
1. Em vez de todos os servidores monitorarem mudanças no znode `/election`, cada servidor monitora o znode imediatamente anterior. Por exemplo:
   - O servidor que cria `/election/leader-00000002` monitora o znode `/election/leader-00000001`.
   - O servidor que cria `/election/leader-00000003` monitora o znode `/election/leader-00000002`.
2. Se o líder atual falhar, apenas o próximo servidor na fila será notificado para assumir a liderança, evitando o efeito de manada.

Essa abordagem é mais eficiente em clusters grandes, pois notifica apenas o servidor necessário.

#### Locks Distribuídos

Se vários servidores precisam acessar um recurso compartilhado, como um arquivo, é necessário garantir acesso exclusivo para evitar inconsistências. O algoritmo de locks distribuídos no Zookeeper é semelhante à eleição de líder:

1. Em vez de criar um znode `/election`, cria-se um znode `/lock`.
2. O servidor que adquire o lock é análogo ao líder na eleição. Ele executa a tarefa necessária e, ao terminar, exclui o znode, permitindo que o próximo servidor adquira o lock.

#### Gerenciamento de Cluster

Manter o gerenciamento de grupos de servidores no Zookeeper é simples, usando znodes persistentes e efêmeros:

1. Crie um znode persistente `/all_nodes` para armazenar informações sobre todos os servidores que se conectam ao cluster.
2. Crie um znode persistente `/live_nodes` para armazenar apenas os servidores ativos, onde cada servidor cria um znode filho efêmero. Se um servidor cair, o Zookeeper exclui automaticamente seu znode filho.
3. Adicione **watches** para monitorar mudanças nos znodes `/all_nodes` e `/live_nodes`. Isso permite que todos os servidores sejam notificados quando um novo servidor ingressa ou sai do cluster, ou quando um servidor ativo se desconecta.

### Visão Geral da Aplicação

Para demonstrar todo o conhecimento teórico na prática, implementaremos uma pequena aplicação Spring Boot que será executada em um cluster gerenciado pelo Apache Zookeeper.

#### Principais Funcionalidades que Construiremos:
- Modelar um banco de dados que é replicado em vários servidores.
- O sistema deve ser escalável horizontalmente, ou seja, se qualquer nova instância de servidor for adicionada ao cluster, ela deve ter os dados mais recentes e começar a servir solicitações de atualização/leitura.
- Consistência de dados. Todas as solicitações de atualização serão encaminhadas para o líder, que então irá transmitir os dados para todos os servidores ativos e, em seguida, retornará o status da atualização.
- Os dados podem ser lidos de qualquer uma das réplicas sem inconsistências.
- Todos os servidores no cluster armazenarão o estado do cluster — Informações como quem é o líder e o estado do servidor (lista de servidores vivos/mortos no cluster). Esta informação é necessária para que o servidor líder transmita solicitações de atualização para servidores ativos, e os servidores seguidores ativos precisam encaminhar qualquer solicitação de atualização para seu líder.
- No caso de uma mudança no estado do cluster (líder cai/qualquer servidor cai), todos os servidores no cluster precisam ser notificados e armazenar a última mudança no armazenamento de dados local do cluster.
- Usaremos o Zookeeper como nosso serviço de coordenação para gerenciar as informações do estado do cluster e notificar todos os servidores no cluster em caso de qualquer mudança no estado do cluster.

#### O que vamos construir:

Três servidores Spring Boot rodando nas portas 8081, 8082 e 8083 serão usados como um banco de dados que armazena os dados de Livros (Lista).

Cada servidor Spring Boot se conecta a um servidor Zookeeper standalone durante a inicialização (localhost:2181).

Cada servidor Spring Boot manterá e armazenará as informações do cluster em sua memória. Essas informações de cluster dirão quais são os servidores ativos atuais, o líder atual do cluster e todos os nós que fazem parte deste cluster.

Criaremos 2 APIs GET, para obter informações sobre o cluster e os dados da pessoa, e 1 API POST para salvar os dados do Livro.

Qualquer solicitação de adição de livro que chegar ao servidor de aplicativos será enviada ao líder, que transmitirá a solicitação de atualização para todos os servidores vivos/seguidores.

Qualquer servidor que reiniciar após ficar inativo sincronizará os dados do Livro a partir do líder.

---

### Implementação da Aplicação

Na implementação, focaremos principalmente em:

1. As operações e algoritmos do Zookeeper que precisamos implementar para resolver o problema de eleição de líder e manter a lista de servidores ativos/inativos.
2. A implementação dos listeners/watchers necessários para que um servidor de aplicativos seja notificado no caso de mudança de líder ou de qualquer servidor cair.
3. As tarefas que nosso servidor de aplicativos Spring Boot (banco de dados) precisa realizar durante a inicialização, como criar nós necessários, registrar watchers, etc.

---

### Operações do Zookeeper

Para criar todos os nós `znodes` necessários (/election, /all_nodes, /live_nodes) antes de iniciar nosso servidor de aplicativos.

#### Criar todos os `znodes` pais persistentes necessários (/election, /all_nodes, /live_nodes)
Isso deve ser feito durante a inicialização da aplicação.

```java
public void createAllParentPersistentNodes() {
        if (!zkClient.exists(ALL_NODES)) {
            zkClient.create(ALL_NODES, "all nodes are displayed here", CreateMode.PERSISTENT);
        }
        if (!zkClient.exists(LIVE_NODES)) {
            zkClient.create(LIVE_NODES, "all live nodes are displayed here", CreateMode.PERSISTENT);
        }
        if (!zkClient.exists(ELECTION_NODE)) {
            zkClient.create(ELECTION_NODE, "all election nodes are displayed here", CreateMode.PERSISTENT);
        }
    }
```

#### Criar `znodes` persistentes dentro de `/all_nodes`

```java
public void createAndAddToAllNodes(String nodeName, String data) {
        if (!zkClient.exists(ALL_NODES)) {
            zkClient.create(ALL_NODES, "all nodes are displayed here", CreateMode.PERSISTENT);
        }

        String childNode = ALL_NODES.concat("/").concat(nodeName);

        if (zkClient.exists(childNode)) {
            return;
        }

        zkClient.create(childNode, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }
```

#### Criar `znodes` efêmeros dentro de `/live_nodes`

```java
 public void createAndAddToLiveNodes(String nodeName, String data) {
        if (!zkClient.exists(LIVE_NODES)) {
            zkClient.create(LIVE_NODES, "all live nodes are displayed here", CreateMode.PERSISTENT);
        }

        String childNode = LIVE_NODES.concat("/").concat(nodeName);

        if (zkClient.exists(childNode)) {
            return;
        }

        zkClient.create(childNode, data, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
    }
```

#### Criar `znodes` sequenciais efêmeros dentro de `/election`

```java
public void createNodeInElectionZnode(String data) {
        if (!zkClient.exists(ELECTION_NODE)) {
            zkClient.create(ELECTION_NODE, "election node", CreateMode.PERSISTENT);
        }

        zkClient.create(ELECTION_NODE.concat("/node"), data, CreateMode.EPHEMERAL_SEQUENTIAL);
    }
```

---

### Eleição de Líder

```java
public String getLeaderNodeData() {
        if (!zkClient.exists(ELECTION_NODE)) {
            throw new RuntimeException("No node /election exists");
        }
        List<String> nodesInElection = zkClient.getChildren(ELECTION_NODE);
        Collections.sort(nodesInElection);

        String masterZNode = nodesInElection.get(0);

        return getZNodeData(ELECTION_NODE.concat("/").concat(masterZNode));
    }
```

---

### Listeners e Watchers

Precisamos configurar quatro listeners diferentes para realizar as ações necessárias quando o estado do cluster mudar.

#### Watcher para qualquer alteração nos filhos de `/all_nodes`, para identificar adição/remoção de servidores no cluster

```java
public class AllClusterNodesChangeListener implements IZkChildListener {

    private final ClusterInformationService clusterInformationService;
    @Override
    public void handleChildChange(String parent, List<String> nodes) {
        log.info("current list of all persistent znodes in the cluster: {}", nodes);

        clusterInformationService.rebuildAllNodesList(nodes);
    }
}
```

#### Watcher para mudança nos filhos de `/live_nodes`, para capturar quando algum servidor cair

```java
public class LiveClusterNodesChangeListener implements IZkChildListener {

    private final ClusterInformationService clusterInformationService;
    @Override
    public void handleChildChange(String parentPath, List<String> currentChildren) {
        log.info("current live size: {}", currentChildren.size());
        clusterInformationService.rebuildLiveNodesList(currentChildren);
    }
}
```

#### Watcher para capturar a mudança de líder, ouvindo a mudança nos filhos do znode `/election`. Em seguida, buscar o znode sequencial menor da lista de filhos e torná-lo o novo servidor líder.

```java
public class MasterChangeListener implements IZkChildListener {

    private final ZookeeperService zookeeperService;
    private final ClusterInformationService clusterInformationService;

    @Override
    public void handleChildChange(String parentPath, List<String> currentChildren) {
        if (currentChildren.isEmpty()) {
            throw new RuntimeException("No node exists to select master!!");
        } else {
            Collections.sort(currentChildren);
            String masterZNode = currentChildren.get(0);

            String masterNode = zookeeperService.getZNodeData(ELECTION_NODE.concat("/").concat(masterZNode));
            log.info("new master is: {}", masterNode);

            clusterInformationService.setMasterNode(masterNode);
        }
    }
}
```

#### Watcher para cada nova sessão estabelecida com o Zookeeper

A sessão da aplicação com o Zookeeper pode terminar se o Zookeeper não receber nenhum ping dentro do tempo limite de sessão configurado, isso pode ocorrer devido a uma falha temporária de rede ou pausa do GC ou qualquer outro motivo.

```java
public class ConnectStateChangeListener implements IZkStateListener {
    private final ZookeeperService zookeeperService;
    private final ClusterInformationService clusterInformationService;
    private final BookService bookService;
    private final RestTemplate restTemplate;
    private final Config config;

    @Override
    public void handleStateChanged(KeeperState keeperState) {
        log.info("current state: {}", keeperState.name());
    }

    @Override
    public void handleNewSession() throws Exception {
        log.info("connected to zookeeper");

        syncDataFromMaster();

        zookeeperService.createAndAddToLiveNodes(config.getHostPort(), "cluster node");

        List<String> liveNodes = zookeeperService.getLiveNodesInZookeeperCluster();
        clusterInformationService.rebuildLiveNodesList(liveNodes);

        zookeeperService.createNodeInElectionZnode(config.getHostPort());
        clusterInformationService.setMasterNode(zookeeperService.getLeaderNodeData());
    }

    @Override
    public void handleSessionEstablishmentError(Throwable throwable) {
        log.error("could not establish zookeeper session");
    }

    private void syncDataFromMaster() {
        if (config.getHostPort().equals(clusterInformationService.getMasterNode())) {
            return;
        }

        String requestUrl = "http://".concat(clusterInformationService.getMasterNode() + "/v1/books/");
        List<Book> books = restTemplate.getForObject(requestUrl, List.class);

        bookService.getAllBooks().clear();
        bookService.addBooks(books);
    }
}
```

---

### Tarefas de Inicialização da Aplicação

Durante a inicialização da aplicação, as seguintes tarefas devem ser executadas:

- Criar todos os `znodes` pais `/election`, `/live_nodes`, `/all_nodes`, se eles não existirem.
- Adicionar o servidor ao cluster criando um `znode` em `/all_nodes`, com o nome `host:port` e atualizar o objeto `ClusterInfo` local.
- Configurar o `znode` sequencial efêmero em `/election`, para configurar um líder para o cluster, com o sufixo “node-” e dados como “host:port”.
- Obter o líder atual do Zookeeper e configurá-lo no objeto `ClusterInfo`.
- Sincronizar todos os dados da Pessoa a partir do servidor líder.
- Uma vez que a sincronização seja concluída, anunciar este servidor como ativo adicionando um `znode` filho em `/live_nodes` com a string `host:port` como nome do `znode` e, em seguida, atualizar o objeto `ClusterInfo`.
- No passo final, registrar todos os listeners/watchers para receber notificações do Zookeeper.

```java
 @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        zookeeperService.createAllParentPersistentNodes();

        addCurrentNodeToAllNodesList();
        addCurrentNodeToElectionNodesList();
        addCurrentNodeToLiveNodesList();

        syncDataFromMaster();

        registerZookeeperWatchers();
    }
```



# Pra rodar

### Inicie a aplicação em diferentes portas (precisa estar com o zkServer on)!
```
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8083"
```
