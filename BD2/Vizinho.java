//Essa classe é usada para agrupar as informações de endereço e status dos vizinhos
class Vizinho {
    private String endereco;
    private String estado;
    private int relogio;
    public Vizinho (String endereco){
        this.endereco = endereco;
        this.estado = "OFFLINE";
        this.relogio = 0;
    }
    public String getEndereco (){
        return this.endereco;
    }
    public String getEstado (){
        return this.estado;
    }
    public void setEstado (String estado){
        this.estado = estado;
    }
    public void setEstado (String estado, int relogio_mensagem){
        if(relogio_mensagem > this.relogio){
            this.estado = estado;
        }
    }
    public int getRelogio (){
        return this.relogio;
    }
    public void setRelogio(int mensagem){
        this.relogio = (this.relogio < mensagem)? mensagem : this.relogio;
    }
    //facilita a impressão das informações do peer
    public void imprime (){
        String imprime_estado = estado;
        if(imprime_estado.equals("ONLINE")){
            imprime_estado = imprime_estado + " ";
        }
        System.out.print(endereco + " " + imprime_estado + " " + "(clock: " + relogio + ")");
    }
}
