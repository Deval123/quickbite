-- Index pour filtrer les commandes par statut
-- Utilisé par le dashboard restaurant (commandes en cours)
CREATE INDEX idx_orders_status ON orders (status);

-- Index composite pour 'mes commandes en cours"
CREATE INDEX idx_orders_user_status ON orders (user_id, status);