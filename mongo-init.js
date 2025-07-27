db = db.getSiblingDB('productdb');
db.createCollection('products');

db = db.getSiblingDB('orderdb');
db.createCollection('orders');

db = db.getSiblingDB('inventorydb');
db.createCollection('inventory');

db = db.getSiblingDB('authdb');
db.createCollection('users');