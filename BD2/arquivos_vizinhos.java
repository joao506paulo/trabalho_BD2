class arquivos_vizinhos {
    private String nome;
    private String vizinho;
    private int tamanho;
    public arquivos_vizinhos (String arquivo, String endereco){
        String [] partes = arquivo.split(":");
        this.nome = partes[0];
        this.tamanho = Integer.parseInt(partes[1]);
        this.vizinho = endereco;
    }
    public String getNomeVizinho(){
        return this.vizinho;
    }
    public String getNome(){
        return this.nome;
    }
    public int getTamanho(){
        return this.tamanho;
    }
}