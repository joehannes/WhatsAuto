# WhatsAuto Makefile
# Convenience targets for common development tasks.

.DEFAULT_GOAL := help
.PHONY: help install dev build release lint fmt fmt-fix test clean

help: ## Show this help message
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

install: ## Install all dependencies
	npm install
	cd src-node && npm install

dev: ## Start development mode (CLJS + Tauri)
	@echo "\033[1mStarting development..."
	@echo "Run 'npx shadow-cljs watch app' in terminal 1"
	@echo "Run 'npx tauri dev' in terminal 2"

cljs-dev: ## Watch ClojureScript (hot reload)
	npx shadow-cljs watch app

cljs-build: ## Production ClojureScript build
	npx shadow-cljs release app

build: cljs-build ## Full production build (CLJS + Tauri)
	npx tauri build

build-deb: cljs-build ## Build Debian package (Linux)
	npx tauri build --bundles deb

release: cljs-build ## Build all release bundles
	npx tauri build

lint: ## Lint ClojureScript with clj-kondo
	npx clj-kondo --lint src || true

fmt: ## Check ClojureScript formatting
	clojure -M:fmt

fmt-fix: ## Auto-fix ClojureScript formatting
	clojure -M:fmt-fix

rust-check: ## Check Rust code (fast)
	cd src-tauri && cargo check

rust-lint: ## Clippy lint Rust code
	cd src-tauri && cargo clippy --all-targets -- -D warnings

rust-fmt: ## Check Rust formatting
	cd src-tauri && cargo fmt -- --check

rust-test: ## Run Rust tests
	cd src-tauri && cargo test

test: rust-test ## Run all tests

node-check: ## Check Node.js sidecar syntax
	node --check src-node/src/index.js

clean: ## Clean build artifacts
	rm -rf dist/js/
	rm -rf .shadow-cljs/
	rm -rf src-tauri/target/
	npx shadow-cljs clean

start-sidecar: ## Start Node sidecar for testing (port 9999)
	cd src-node && node src/index.js --port 9999

db-shell: ## Open SQLite shell on development database
	sqlite3 ~/.local/share/com.joehannes.whatsauto/whatsauto.db
