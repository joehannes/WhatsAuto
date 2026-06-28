//! SQLite persistence layer.
//!
//! Wraps sqlx with an async `Database` handle and migration support.
//! Application logic must only interact with this module through
//! the defined repository traits — never raw SQL outside this module.

use anyhow::Result;
use sqlx::{sqlite::SqlitePoolOptions, SqlitePool};
use std::path::Path;
use tracing::info;

pub mod chat_repo;
pub mod contact_repo;
pub mod message_repo;
pub mod settings_repo;
pub mod ai_repo;

/// Shared database handle (connection pool).
#[derive(Clone, Debug)]
pub struct Database {
    pub pool: SqlitePool,
}

impl Database {
    /// Open (or create) the SQLite database at the given path.
    pub async fn connect(path: &Path) -> Result<Self> {
        let url = format!("sqlite://{}?mode=rwc", path.display());
        info!("Connecting to SQLite: {}", url);

        let pool = SqlitePoolOptions::new()
            .max_connections(5)
            .connect(&url)
            .await?;

        Ok(Self { pool })
    }

    /// Run embedded migrations.
    pub async fn migrate(&self) -> Result<()> {
        info!("Running database migrations");
        sqlx::migrate!("./migrations").run(&self.pool).await?;
        Ok(())
    }
}
