-- Catálogo inicial de serviços e itens de estoque para demonstração

INSERT INTO servicos (id, nome, descricao, preco, tempo_estimado_minutos, ativo, versao) VALUES
    ('7b1e2f00-0000-4000-8000-000000000001', 'Troca de óleo', 'Substituição do óleo do motor e do filtro', 80.00, 40, TRUE, 0),
    ('7b1e2f00-0000-4000-8000-000000000002', 'Alinhamento', 'Alinhamento da direção', 120.00, 60, TRUE, 0),
    ('7b1e2f00-0000-4000-8000-000000000003', 'Balanceamento', 'Balanceamento das quatro rodas', 100.00, 50, TRUE, 0),
    ('7b1e2f00-0000-4000-8000-000000000004', 'Revisão de freios', 'Inspeção e troca de pastilhas', 150.00, 90, TRUE, 0);

INSERT INTO pecas (id, codigo, nome, descricao, tipo, preco_unitario, quantidade_estoque, estoque_minimo, versao) VALUES
    ('8c2f3a00-0000-4000-8000-000000000001', 'OLEO-5W30', 'Óleo 5W30 (litro)', 'Óleo sintético 5W30', 'INSUMO', 45.00, 100, 20, 0),
    ('8c2f3a00-0000-4000-8000-000000000002', 'FILTRO-OLEO', 'Filtro de óleo', 'Filtro de óleo universal', 'PECA', 35.00, 30, 5, 0),
    ('8c2f3a00-0000-4000-8000-000000000003', 'PASTILHA-FREIO', 'Jogo de pastilhas de freio', 'Pastilhas dianteiras', 'PECA', 180.00, 10, 2, 0),
    ('8c2f3a00-0000-4000-8000-000000000004', 'FLUIDO-FREIO', 'Fluido de freio DOT4 (500ml)', 'Fluido de freio', 'INSUMO', 32.00, 25, 5, 0);
