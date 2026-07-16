-- ============================================================
-- V3 : Seed data - 5 restaurants avec leurs menus
-- Donnees de demo pour la serie "System Design en 10 min"
-- ============================================================

-- On utilise des UUIDs fixes pour pouvoir referencer les restaurants
-- dans les menu_items et dans les tests curl.

-- ============================================================
-- RESTAURANTS
-- ============================================================

INSERT INTO restaurants (id, owner_id, name, description, cuisine_type, address, phone, latitude, longitude, opening_time, closing_time, active, avg_rating)
VALUES
    ('a1b2c3d4-0001-4000-8000-000000000001', 'restaurant1', 'Le Petit Bistrot',
     'Cuisine francaise traditionnelle, produits frais du marche',
     'Francais', '12 Rue de Rivoli, 75001 Paris', '+33 1 42 36 12 00',
     48.8566, 2.3522, '11:30', '22:30', true, 4.5),

    ('a1b2c3d4-0001-4000-8000-000000000002', 'restaurant1', 'Tokyo Ramen House',
     'Ramen authentiques, bouillon mijote 12h, nouilles fraiches',
     'Japonais', '45 Rue Sainte-Anne, 75002 Paris', '+33 1 47 03 38 59',
     48.8676, 2.3372, '11:00', '23:00', true, 4.7),

    ('a1b2c3d4-0001-4000-8000-000000000003', 'restaurant1', 'Pizza Napoli',
     'Pizzas napolitaines au feu de bois, mozzarella di bufala',
     'Italien', '8 Rue de la Huchette, 75005 Paris', '+33 1 43 26 44 11',
     48.8527, 2.3470, '12:00', '23:30', true, 4.3),

    ('a1b2c3d4-0001-4000-8000-000000000004', 'restaurant1', 'Le Kebab du Quartier',
     'Kebabs, falafels et mezzes maison, viande rotie a la broche',
     'Turc', '22 Rue du Faubourg Saint-Denis, 75010 Paris', '+33 1 48 24 86 42',
     48.8722, 2.3553, '10:00', '02:00', true, 4.1),

    ('a1b2c3d4-0001-4000-8000-000000000005', 'restaurant1', 'Chez Mama Africa',
     'Cuisine africaine, mafe, yassa, thieboudienne, grillades',
     'Africain', '15 Rue Dejean, 75018 Paris', '+33 1 42 52 89 33',
     48.8849, 2.3499, '12:00', '22:00', true, 4.6);

-- ============================================================
-- MENU ITEMS - Le Petit Bistrot
-- ============================================================

INSERT INTO menu_items (id, restaurant_id, name, description, price, category, available)
VALUES
    ('b1b2c3d4-0001-4000-8000-000000000001', 'a1b2c3d4-0001-4000-8000-000000000001',
     'Steak-frites', 'Entrecote de boeuf, frites maison, sauce bearnaise', 18.50, 'Plat', true),
    ('b1b2c3d4-0001-4000-8000-000000000002', 'a1b2c3d4-0001-4000-8000-000000000001',
     'Croque-monsieur', 'Jambon, gruyere, bechamel, salade verte', 12.00, 'Plat', true),
    ('b1b2c3d4-0001-4000-8000-000000000003', 'a1b2c3d4-0001-4000-8000-000000000001',
     'Soupe a l''oignon', 'Gratinee au fromage, croutons', 9.50, 'Entree', true),
    ('b1b2c3d4-0001-4000-8000-000000000004', 'a1b2c3d4-0001-4000-8000-000000000001',
     'Tarte Tatin', 'Pommes caramelisees, pate feuilletee, creme fraiche', 8.00, 'Dessert', true),
    ('b1b2c3d4-0001-4000-8000-000000000005', 'a1b2c3d4-0001-4000-8000-000000000001',
     'Salade Nicoise', 'Thon, olives, oeufs, haricots verts, anchois', 14.00, 'Entree', false);

-- ============================================================
-- MENU ITEMS - Tokyo Ramen House
-- ============================================================

INSERT INTO menu_items (id, restaurant_id, name, description, price, category, available)
VALUES
    ('b1b2c3d4-0002-4000-8000-000000000001', 'a1b2c3d4-0001-4000-8000-000000000002',
     'Tonkotsu Ramen', 'Bouillon porc 12h, chashu, oeuf mollet, nori', 14.50, 'Ramen', true),
    ('b1b2c3d4-0002-4000-8000-000000000002', 'a1b2c3d4-0001-4000-8000-000000000002',
     'Miso Ramen', 'Bouillon miso, porc hache, mais, beurre', 13.50, 'Ramen', true),
    ('b1b2c3d4-0002-4000-8000-000000000003', 'a1b2c3d4-0001-4000-8000-000000000002',
     'Gyoza (x6)', 'Raviolis japonais grilles, porc et legumes', 7.50, 'Entree', true),
    ('b1b2c3d4-0002-4000-8000-000000000004', 'a1b2c3d4-0001-4000-8000-000000000002',
     'Edamame', 'Feves de soja, fleur de sel', 4.50, 'Entree', true),
    ('b1b2c3d4-0002-4000-8000-000000000005', 'a1b2c3d4-0001-4000-8000-000000000002',
     'Matcha Latte', 'The vert matcha, lait mousse', 5.00, 'Boisson', true);

-- ============================================================
-- MENU ITEMS - Pizza Napoli
-- ============================================================

INSERT INTO menu_items (id, restaurant_id, name, description, price, category, available)
VALUES
    ('b1b2c3d4-0003-4000-8000-000000000001', 'a1b2c3d4-0001-4000-8000-000000000003',
     'Margherita', 'Tomate San Marzano, mozzarella di bufala, basilic', 11.00, 'Pizza', true),
    ('b1b2c3d4-0003-4000-8000-000000000002', 'a1b2c3d4-0001-4000-8000-000000000003',
     'Quattro Formaggi', 'Mozzarella, gorgonzola, parmesan, chevre', 13.50, 'Pizza', true),
    ('b1b2c3d4-0003-4000-8000-000000000003', 'a1b2c3d4-0001-4000-8000-000000000003',
     'Diavola', 'Tomate, mozzarella, salami piquant, piments', 12.50, 'Pizza', true),
    ('b1b2c3d4-0003-4000-8000-000000000004', 'a1b2c3d4-0001-4000-8000-000000000003',
     'Tiramisu', 'Mascarpone, cafe, biscuits, cacao', 7.50, 'Dessert', true),
    ('b1b2c3d4-0003-4000-8000-000000000005', 'a1b2c3d4-0001-4000-8000-000000000003',
     'Calzone', 'Ricotta, jambon, champignons, mozzarella', 13.00, 'Pizza', false);

-- ============================================================
-- MENU ITEMS - Le Kebab du Quartier
-- ============================================================

INSERT INTO menu_items (id, restaurant_id, name, description, price, category, available)
VALUES
    ('b1b2c3d4-0004-4000-8000-000000000001', 'a1b2c3d4-0001-4000-8000-000000000004',
     'Kebab Poulet', 'Pain pita, poulet marine, salade, sauce blanche', 8.50, 'Kebab', true),
    ('b1b2c3d4-0004-4000-8000-000000000002', 'a1b2c3d4-0001-4000-8000-000000000004',
     'Kebab Mixte', 'Pain pita, agneau + poulet, salade, harissa', 9.50, 'Kebab', true),
    ('b1b2c3d4-0004-4000-8000-000000000003', 'a1b2c3d4-0001-4000-8000-000000000004',
     'Assiette Falafel', 'Falafels maison, houmous, tabboule, pain', 10.00, 'Assiette', true),
    ('b1b2c3d4-0004-4000-8000-000000000004', 'a1b2c3d4-0001-4000-8000-000000000004',
     'Durum Veau', 'Galette, veau grille, crudites, sauce samourai', 9.00, 'Kebab', true),
    ('b1b2c3d4-0004-4000-8000-000000000005', 'a1b2c3d4-0001-4000-8000-000000000004',
     'Baklava (x3)', 'Patisserie turque, pistaches, miel', 5.00, 'Dessert', true);

-- ============================================================
-- MENU ITEMS - Chez Mama Africa
-- ============================================================

INSERT INTO menu_items (id, restaurant_id, name, description, price, category, available)
VALUES
    ('b1b2c3d4-0005-4000-8000-000000000001', 'a1b2c3d4-0001-4000-8000-000000000005',
     'Mafe Poulet', 'Poulet mijoté, sauce arachide, riz blanc', 13.50, 'Plat', true),
    ('b1b2c3d4-0005-4000-8000-000000000002', 'a1b2c3d4-0001-4000-8000-000000000005',
     'Yassa Poisson', 'Poisson grille, oignons caramelises, citron, riz', 14.50, 'Plat', true),
    ('b1b2c3d4-0005-4000-8000-000000000003', 'a1b2c3d4-0001-4000-8000-000000000005',
     'Thieboudienne', 'Riz au poisson, legumes, tomate, le plat national', 15.00, 'Plat', true),
    ('b1b2c3d4-0005-4000-8000-000000000004', 'a1b2c3d4-0001-4000-8000-000000000005',
     'Alloco', 'Bananes plantain frites, piment, oignons', 6.50, 'Accompagnement', true),
    ('b1b2c3d4-0005-4000-8000-000000000005', 'a1b2c3d4-0001-4000-8000-000000000005',
     'Bissap', 'Jus d''hibiscus frais, menthe, sucre', 4.00, 'Boisson', true);
