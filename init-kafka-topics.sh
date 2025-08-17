#!/bin/bash

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
sleep 30

# Create Kafka topics
echo "Creating Kafka topics..."

# Order topics
kafka-topics.sh --create --topic order-created --bootstrap-server kafka:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic order-updated --bootstrap-server kafka:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic order-cancelled --bootstrap-server kafka:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic order-completed --bootstrap-server kafka:9092 --partitions 3 --replication-factor 1

# Product topics
kafka-topics.sh --create --topic product-created --bootstrap-server kafka:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic product-updated --bootstrap-server kafka:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic product-deleted --bootstrap-server kafka:9092 --partitions 3 --replication-factor 1

# Inventory topics
kafka-topics.sh --create --topic inventory-updated --bootstrap-server kafka:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic inventory-low-stock --bootstrap-server kafka:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic inventory-out-of-stock --bootstrap-server kafka:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic inventory-restocked --bootstrap-server kafka:9092 --partitions 3 --replication-factor 1

echo "Kafka topics created successfully!"

# List topics to verify
kafka-topics.sh --list --bootstrap-server kafka:9092