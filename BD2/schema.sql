-- ##########################################################################
-- Script SQL para Criação de Tabelas em PostgreSQL
-- Sistema de Compartilhamento e Armazenamento de Arquivos em Rede
-- ##########################################################################

-- --------------------------------------------------------------------------
-- 1. Tabela de Usuários
-- Armazena as informações básicas dos usuários do sistema.
-- --------------------------------------------------------------------------
CREATE TABLE usuarios (
    id_usuario SERIAL PRIMARY KEY,
    nome_usuario VARCHAR(100) NOT NULL UNIQUE, -- Nome único para login
    email VARCHAR(255) UNIQUE, -- Email para recuperação de senha, etc.
    senha_hash VARCHAR(255) NOT NULL, -- Hash da senha (NUNCA armazene a senha em texto puro!)
    salt_senha VARCHAR(255) NOT NULL, -- Salt para aumentar a segurança do hash da senha
    data_registro TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ultimo_login TIMESTAMP WITH TIME ZONE,
    status_conta VARCHAR(50) DEFAULT 'ativo' -- Ex: 'ativo', 'inativo', 'bloqueado'
);

-- --------------------------------------------------------------------------
-- 2. Tabela de Grupos de Usuários (Opcional, mas Recomendado)
-- Permite organizar usuários em grupos para gerenciamento de permissões.
-- --------------------------------------------------------------------------
CREATE TABLE grupos (
    id_grupo SERIAL PRIMARY KEY,
    nome_grupo VARCHAR(100) NOT NULL UNIQUE,
    descricao TEXT
);

-- --------------------------------------------------------------------------
-- 3. Tabela de Ligação Usuários_Grupos (Muitos-para-Muitos)
-- Conecta usuários aos grupos aos quais pertencem.
-- --------------------------------------------------------------------------
CREATE TABLE usuarios_grupos (
    id_usuario_grupo SERIAL PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_grupo INT NOT NULL,
    CONSTRAINT fk_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuarios (id_usuario)
        ON DELETE CASCADE, -- Se o usuário for deletado, a associação é deletada
    CONSTRAINT fk_grupo
        FOREIGN KEY (id_grupo)
        REFERENCES grupos (id_grupo)
        ON DELETE CASCADE, -- Se o grupo for deletado, a associação é deletada
    UNIQUE (id_usuario, id_grupo) -- Garante que um usuário não seja adicionado ao mesmo grupo duas vezes
);

-- --------------------------------------------------------------------------
-- 4. Tabela de Arquivos (Dados Mais Recentes)
-- Armazena a versão mais recente dos arquivos, metadados e o conteúdo em base64.
-- --------------------------------------------------------------------------
CREATE TABLE arquivos (
    id_arquivo SERIAL PRIMARY KEY,
    nome_arquivo VARCHAR(255) NOT NULL,
    conteudo_base64 TEXT NOT NULL, -- Conteúdo do arquivo em base64 (CUIDADO com arquivos muito grandes)
    tamanho_arquivo BIGINT NOT NULL, -- Tamanho em bytes
    checksum_md5 VARCHAR(32), -- MD5 para verificação de integridade
    id_autor_original INT NOT NULL, -- Quem fez o upload original
    data_criacao TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    data_ultima_modificacao TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    versao_atual INT DEFAULT 1, -- Controla a versão atual do arquivo
    CONSTRAINT fk_autor_original
        FOREIGN KEY (id_autor_original)
        REFERENCES usuarios (id_usuario)
        ON DELETE RESTRICT -- Não permite deletar o autor original se houver arquivos associados
);

-- --------------------------------------------------------------------------
-- 5. Tabela de Versões Anteriores dos Arquivos
-- Armazena as versões anteriores dos arquivos.
-- --------------------------------------------------------------------------
CREATE TABLE versoes_arquivos (
    id_versao_arquivo SERIAL PRIMARY KEY,
    id_arquivo INT NOT NULL,
    numero_versao INT NOT NULL,
    conteudo_base64_versao TEXT NOT NULL, -- Conteúdo da versão em base64
    tamanho_versao BIGINT NOT NULL,
    checksum_md5_versao VARCHAR(32),
    id_autor_versao INT NOT NULL, -- Quem criou esta versão
    data_versao TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_arquivo_versao
        FOREIGN KEY (id_arquivo)
        REFERENCES arquivos (id_arquivo)
        ON DELETE CASCADE, -- Se o arquivo principal for deletado, suas versões também são
    CONSTRAINT fk_autor_versao
        FOREIGN KEY (id_autor_versao)
        REFERENCES usuarios (id_usuario)
        ON DELETE RESTRICT,
    UNIQUE (id_arquivo, numero_versao) -- Garante que não haja duas versões com o mesmo número para o mesmo arquivo
);

-- --------------------------------------------------------------------------
-- 6. Tabela de Permissões de Acesso (Quem Pode Acessar Cada Dado)
-- Define quais usuários ou grupos têm acesso a quais arquivos.
-- --------------------------------------------------------------------------
CREATE TABLE permissoes_acesso (
    id_permissao_acesso SERIAL PRIMARY KEY,
    id_arquivo INT NOT NULL,
    id_usuario INT, -- Pode ser nulo se a permissão for para um grupo
    id_grupo INT, -- Pode ser nulo se a permissão for para um usuário individual
    nivel_acesso VARCHAR(50) NOT NULL, -- Ex: 'leitura', 'escrita', 'download', 'visualizacao'
    data_concessao TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    data_expiracao TIMESTAMP WITH TIME ZONE, -- Opcional: para permissões temporárias
    CONSTRAINT fk_arquivo_permissao
        FOREIGN KEY (id_arquivo)
        REFERENCES arquivos (id_arquivo)
        ON DELETE CASCADE,
    CONSTRAINT fk_usuario_permissao
        FOREIGN KEY (id_usuario)
        REFERENCES usuarios (id_usuario)
        ON DELETE CASCADE,
    CONSTRAINT fk_grupo_permissao
        FOREIGN KEY (id_grupo)
        REFERENCES grupos (id_grupo)
        ON DELETE CASCADE,
    CONSTRAINT chk_usuario_ou_grupo_not_null
        CHECK ( (id_usuario IS NOT NULL AND id_grupo IS NULL) OR (id_usuario IS NULL AND id_grupo IS NOT NULL) ),
    -- Garante que ou id_usuario ou id_grupo (mas não ambos) seja preenchido
    UNIQUE (id_arquivo, id_usuario, id_grupo) -- Evita permissões duplicadas para o mesmo arquivo/usuário/grupo
);

-- --------------------------------------------------------------------------
-- 7. Tabela de Compartilhamentos Ativos
-- Gerencia os compartilhamentos específicos que foram feitos.
-- --------------------------------------------------------------------------
CREATE TABLE compartilhamentos (
    id_compartilhamento SERIAL PRIMARY KEY,
    id_arquivo INT NOT NULL,
    id_usuario_origem INT NOT NULL, -- Quem iniciou o compartilhamento
    id_usuario_destino INT, -- Destinatário do compartilhamento (pode ser nulo se for via link público, etc.)
    id_grupo_destino INT, -- Destinatário do compartilhamento (pode ser nulo se for para usuário individual)
    tipo_compartilhamento VARCHAR(50) NOT NULL, -- Ex: 'link_publico', 'usuario_especifico', 'grupo'
    token_compartilhamento VARCHAR(255) UNIQUE, -- Para links públicos (opcional)
    data_compartilhamento TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    data_expiracao TIMESTAMP WITH TIME ZONE, -- Opcional: para compartilhamentos com prazo
    status_compartilhamento VARCHAR(50) DEFAULT 'ativo', -- Ex: 'ativo', 'revogado', 'expirado'
    CONSTRAINT fk_arquivo_compartilhamento
        FOREIGN KEY (id_arquivo)
        REFERENCES arquivos (id_arquivo)
        ON DELETE CASCADE,
    CONSTRAINT fk_usuario_origem_compartilhamento
        FOREIGN KEY (id_usuario_origem)
        REFERENCES usuarios (id_usuario)
        ON DELETE RESTRICT,
    CONSTRAINT fk_usuario_destino_compartilhamento
        FOREIGN KEY (id_usuario_destino)
        REFERENCES usuarios (id_usuario)
        ON DELETE CASCADE,
    CONSTRAINT fk_grupo_destino_compartilhamento
        FOREIGN KEY (id_grupo_destino)
        REFERENCES grupos (id_grupo)
        ON DELETE CASCADE,
    CONSTRAINT chk_destino_compartilhamento
        CHECK ( (id_usuario_destino IS NOT NULL AND id_grupo_destino IS NULL) OR
                (id_usuario_destino IS NULL AND id_grupo_destino IS NOT NULL) OR
                (tipo_compartilhamento = 'link_publico' AND id_usuario_destino IS NULL AND id_grupo_destino IS NULL) )
    -- Garante um destinatário ou que seja um link público sem destinatário específico.
);

-- --------------------------------------------------------------------------
-- 8. Tabela de Logs de Atividade/Auditoria
-- Rastreia todas as ações importantes no sistema.
-- --------------------------------------------------------------------------
CREATE TABLE logs_atividade (
    id_log SERIAL PRIMARY KEY,
    id_usuario INT, -- Usuário que realizou a ação (pode ser nulo para eventos do sistema)
    id_arquivo INT, -- Arquivo envolvido (pode ser nulo para eventos não relacionados a arquivos)
    tipo_evento VARCHAR(100) NOT NULL, -- Ex: 'upload', 'download', 'visualizacao', 'compartilhamento', 'login'
    detalhes TEXT, -- Informações adicionais sobre o evento
    data_hora TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    endereco_ip VARCHAR(45), -- IP do cliente (para segurança e auditoria)
    CONSTRAINT fk_usuario_log
        FOREIGN KEY (id_usuario)
        REFERENCES usuarios (id_usuario)
        ON DELETE SET NULL, -- Se o usuário for deletado, o ID no log se torna nulo
    CONSTRAINT fk_arquivo_log
        FOREIGN KEY (id_arquivo)
        REFERENCES arquivos (id_arquivo)
        ON DELETE SET NULL -- Se o arquivo for deletado, o ID no log se torna nulo
);
