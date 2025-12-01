.PHONY: help install run docker-up docker-down docker-logs docker-restart clean test example

help:
	@echo "Bankao API - Available Commands:"
	@echo ""
	@echo "  make install         - Install Python dependencies"
	@echo "  make run             - Run the API locally (requires MariaDB)"
	@echo "  make docker-up       - Start API with Docker Compose"
	@echo "  make docker-down     - Stop Docker Compose services"
	@echo "  make docker-logs     - View Docker Compose logs"
	@echo "  make docker-restart  - Restart Docker Compose services"
	@echo "  make example         - Run example usage script"
	@echo "  make test            - Run race condition tests"
	@echo "  make clean           - Remove cache files"
	@echo ""

install:
	@echo "Installing dependencies..."
	pip install -r requirements.txt
	@echo "✓ Dependencies installed"

run:
	@echo "Starting Bankao API..."
	@echo "Note: Make sure MariaDB is running and configured"
	python main.py

docker-up:
	@echo "Starting services with Docker Compose..."
	docker-compose up -d
	@echo ""
	@echo "✓ Services started"
	@echo "  API: http://localhost:8000"
	@echo "  Docs: http://localhost:8000/docs"
	@echo ""
	@echo "To view logs and get TOTP secrets:"
	@echo "  make docker-logs"

docker-down:
	@echo "Stopping Docker Compose services..."
	docker-compose down
	@echo "✓ Services stopped"

docker-logs:
	@echo "Showing logs (press Ctrl+C to exit)..."
	docker-compose logs -f

docker-restart:
	@echo "Restarting services..."
	docker-compose restart
	@echo "✓ Services restarted"

example:
	@echo "Running example usage script..."
	@echo "Note: Make sure API is running first (make docker-up)"
	python example_usage.py

test:
	@echo "Running race condition tests..."
	@echo "Note: Make sure API is running first (make docker-up)"
	python test_race_conditions.py

clean:
	@echo "Cleaning cache files..."
	find . -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
	find . -type f -name "*.pyc" -delete 2>/dev/null || true
	find . -type f -name "*.pyo" -delete 2>/dev/null || true
	find . -type d -name "*.egg-info" -exec rm -rf {} + 2>/dev/null || true
	@echo "✓ Cache files removed"
