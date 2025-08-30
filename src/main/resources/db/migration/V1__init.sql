-- Notification Hub API Schema Migration V1

-- Notification Templates
CREATE TABLE notification_templates (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    subject TEXT,
    body TEXT NOT NULL,
    channel VARCHAR(50) NOT NULL,
    language VARCHAR(10),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notification_templates_code ON notification_templates(code);
CREATE INDEX idx_notification_templates_channel ON notification_templates(channel);
CREATE INDEX idx_notification_templates_status ON notification_templates(status);

-- Notification Preferences
CREATE TABLE notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    timezone VARCHAR(100) NOT NULL DEFAULT 'UTC',
    default_priority VARCHAR(50) NOT NULL DEFAULT 'NORMAL',
    version BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notification_preferences_user_id ON notification_preferences(user_id);

-- Preference Enabled Channels (many-to-many)
CREATE TABLE preference_enabled_channels (
    preference_id BIGINT NOT NULL,
    channel VARCHAR(50) NOT NULL,
    PRIMARY KEY (preference_id, channel),
    FOREIGN KEY (preference_id) REFERENCES notification_preferences(id) ON DELETE CASCADE
);

-- Preference Opted Out Types (many-to-many)
CREATE TABLE preference_opted_out_types (
    preference_id BIGINT NOT NULL,
    notification_type VARCHAR(255) NOT NULL,
    PRIMARY KEY (preference_id, notification_type),
    FOREIGN KEY (preference_id) REFERENCES notification_preferences(id) ON DELETE CASCADE
);

-- Notifications
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    notification_type VARCHAR(255) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    recipient VARCHAR(500) NOT NULL,
    subject TEXT,
    content TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    scheduled_at TIMESTAMP WITH TIME ZONE,
    sent_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    read_at TIMESTAMP WITH TIME ZONE,
    template_id BIGINT,
    priority VARCHAR(50) NOT NULL DEFAULT 'NORMAL',
    version BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (template_id) REFERENCES notification_templates(id)
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_channel ON notifications(channel);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE INDEX idx_notifications_scheduled_at ON notifications(scheduled_at);

-- Notification Metadata (key-value pairs)
CREATE TABLE notification_metadata (
    notification_id BIGINT NOT NULL,
    meta_key VARCHAR(255) NOT NULL,
    meta_value TEXT,
    PRIMARY KEY (notification_id, meta_key),
    FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE
);

-- Webhook Endpoints
CREATE TABLE webhook_endpoints (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    url VARCHAR(500) NOT NULL,
    secret VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    max_retries INTEGER DEFAULT 5,
    timeout_seconds INTEGER DEFAULT 30,
    version BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_webhook_endpoints_user_id ON webhook_endpoints(user_id);
CREATE INDEX idx_webhook_endpoints_status ON webhook_endpoints(status);

-- Webhook Events (many-to-many)
CREATE TABLE webhook_events (
    webhook_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    PRIMARY KEY (webhook_id, event_type),
    FOREIGN KEY (webhook_id) REFERENCES webhook_endpoints(id) ON DELETE CASCADE
);

-- Webhook Deliveries
CREATE TABLE webhook_deliveries (
    id BIGSERIAL PRIMARY KEY,
    webhook_id BIGINT NOT NULL,
    notification_id BIGINT,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT,
    status_code INTEGER DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    attempt_count INTEGER DEFAULT 0,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    delivered_at TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (webhook_id) REFERENCES webhook_endpoints(id),
    FOREIGN KEY (notification_id) REFERENCES notifications(id)
);

CREATE INDEX idx_webhook_deliveries_webhook_id ON webhook_deliveries(webhook_id);
CREATE INDEX idx_webhook_deliveries_status ON webhook_deliveries(status);
CREATE INDEX idx_webhook_deliveries_next_retry_at ON webhook_deliveries(next_retry_at);

-- Notification Audit
CREATE TABLE notification_audit (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    details TEXT,
    previous_status VARCHAR(50),
    new_status VARCHAR(50),
    performed_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE
);

CREATE INDEX idx_notification_audit_notification_id ON notification_audit(notification_id);
CREATE INDEX idx_notification_audit_created_at ON notification_audit(created_at);
