-- ============================================================================
-- PHASE 7: SCHEMA COMPLETION & SPEC ONTOLOGY SEED DATA
-- Reference: 07-database-schema.md
-- ============================================================================

-- 1. Add consent_training_data column to users table if not exists
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS consent_training_data BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Add human_verified column to spec_ontology if not exists
ALTER TABLE spec_ontology 
ADD COLUMN IF NOT EXISTS human_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- 3. Add category index on spec_ontology
CREATE INDEX IF NOT EXISTS idx_spec_ontology_category ON spec_ontology(category);

-- 4. Add partial index on shadow_predictions for disagreements
CREATE INDEX IF NOT EXISTS idx_shadow_disagreements 
ON shadow_predictions(model_name, agreed) 
WHERE agreed = FALSE;

-- 5. Seed canonical specification ontology (hardware, software, system specs)
INSERT INTO spec_ontology (canonical_name, display_name, category, default_unit, default_operator, synonyms, usage_count, human_verified)
VALUES
    ('ram_capacity', 'RAM Capacity', 'memory', 'GB', '>=', ARRAY['RAM', 'Memory', 'System Memory', 'RAM Size', 'Main Memory'], 10, TRUE),
    ('ram_type', 'RAM Technology', 'memory', NULL, '==', ARRAY['RAM Type', 'Memory Type', 'DDR Type', 'Generation'], 10, TRUE),
    ('ram_speed', 'RAM Frequency', 'memory', 'MHz', '>=', ARRAY['Memory Speed', 'Clock Speed', 'RAM Speed', 'Bus Speed'], 5, TRUE),
    ('storage_capacity', 'Storage Capacity', 'storage', 'GB', '>=', ARRAY['Storage', 'SSD', 'HDD', 'Hard Drive', 'Internal Storage', 'Disk Capacity'], 10, TRUE),
    ('storage_type', 'Storage Type', 'storage', NULL, '==', ARRAY['Drive Type', 'Disk Type', 'Storage Interface', 'SSD/HDD'], 10, TRUE),
    ('cpu_cores', 'Processor Cores', 'processor', NULL, '>=', ARRAY['Cores', 'Core Count', 'CPU Cores', 'Number of Cores'], 8, TRUE),
    ('cpu_threads', 'Processor Threads', 'processor', NULL, '>=', ARRAY['Threads', 'Thread Count', 'Logical Processors'], 6, TRUE),
    ('cpu_clock_speed', 'Processor Base Clock', 'processor', 'GHz', '>=', ARRAY['Processor Speed', 'Base Clock', 'CPU Frequency', 'Clock Speed'], 8, TRUE),
    ('cpu_turbo_speed', 'Processor Boost Clock', 'processor', 'GHz', '>=', ARRAY['Turbo Boost', 'Max Turbo', 'Boost Clock'], 6, TRUE),
    ('display_size', 'Display Screen Size', 'display', 'inches', '==', ARRAY['Screen Size', 'Monitor Size', 'Diagonal Size'], 8, TRUE),
    ('display_resolution', 'Display Resolution', 'display', NULL, '==', ARRAY['Resolution', 'Native Resolution', 'Screen Resolution'], 8, TRUE),
    ('display_refresh_rate', 'Display Refresh Rate', 'display', 'Hz', '>=', ARRAY['Refresh Rate', 'Panel Refresh'], 4, TRUE),
    ('battery_life', 'Battery Operating Time', 'power', 'hours', '>=', ARRAY['Battery Life', 'Run Time', 'Operating Time', 'Autonomy'], 7, TRUE),
    ('battery_capacity', 'Battery Capacity', 'power', 'Wh', '>=', ARRAY['Battery Size', 'Watt Hours', 'Whr'], 5, TRUE),
    ('device_weight', 'Device Weight', 'physical', 'kg', '<=', ARRAY['Weight', 'Gross Weight', 'Mass', 'System Weight'], 8, TRUE),
    ('operating_system', 'Operating System', 'software', NULL, '==', ARRAY['OS', 'Platform', 'Preinstalled OS'], 9, TRUE),
    ('warranty_duration', 'Warranty Duration', 'service', 'years', '>=', ARRAY['Warranty', 'Warranty Period', 'Support Duration', 'Guaranteed Service'], 9, TRUE),
    ('ethernet_speed', 'Ethernet Interface Speed', 'networking', 'Gbps', '>=', ARRAY['LAN Speed', 'Ethernet', 'RJ45 Speed', 'Network Port'], 6, TRUE),
    ('wifi_standard', 'Wi-Fi Standard', 'networking', NULL, '==', ARRAY['WiFi', 'Wireless', 'WLAN Standard', 'Wi-Fi Generation'], 7, TRUE),
    ('bluetooth_version', 'Bluetooth Version', 'networking', NULL, '>=', ARRAY['Bluetooth', 'BT Version'], 5, TRUE),
    ('ip_rating', 'Ingress Protection Rating', 'physical', NULL, '>=', ARRAY['IP Rating', 'Water Resistance', 'Dust Resistance', 'Enclosure Protection'], 6, TRUE)
ON CONFLICT (canonical_name) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    category = EXCLUDED.category,
    default_unit = EXCLUDED.default_unit,
    default_operator = EXCLUDED.default_operator,
    synonyms = EXCLUDED.synonyms,
    human_verified = EXCLUDED.human_verified;
