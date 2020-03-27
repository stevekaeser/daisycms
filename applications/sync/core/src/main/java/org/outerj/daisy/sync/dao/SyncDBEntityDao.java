/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.outerj.daisy.sync.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.sync.Attribute;
import org.outerj.daisy.sync.AttributeImpl;
import org.outerj.daisy.sync.AttributeType;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.EntityImpl;
import org.outerj.daisy.sync.EntityNotFoundException;
import org.outerj.daisy.sync.SyncState;
import org.outerj.daisy.sync.VariantHelper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class SyncDBEntityDao implements SyncEntityDao {

    private final String getLastId = "select LAST_INSERT_ID()";

    private final String insertExtSyncEntity = "insert into sync_ext (ext_id, entity_name, ext_last_modified, ext_deleted, update_ts, state, language) values (?, ?, ?, ?, ?, ?, ?)";

    private final String updateSyncEntity = "update sync_ext as ext, sync_dsy as dsy set ext.ext_last_modified = ?, ext.ext_deleted = ?, ext.update_ts = ?, ext.state = ?, dsy.dsy_version = ?, dsy.dsy_deleted = ? where dsy.dsy_var_key = ? and ext.id = dsy.sync_id";

    private final String replaceDaisySyncEntity = "update sync_dsy as dsy set dsy.dsy_var_key = ?, dsy.dsy_version = ?, dsy.dsy_deleted = ? where dsy.dsy_var_key = ?";

    private final String insertDaisySyncEntity = "insert into sync_dsy (sync_id, dsy_var_key, dsy_version, dsy_deleted ) values (?, ?, ?, ?)";

    private final String insertAttribute = "insert into sync_value (dsy_var_key, dsy_field_name, dsy_attribute_type, i, value) values (?, ?, ?, ?, ?)";

    // private final String updateAttribute = "update sync_value set value = ?
    // where dsy_var_key = ? and dsy_field_name = ? and i = ?";

    private final String deleteAttributes = "delete from sync_value where dsy_var_key = ? and dsy_field_name = ? and dsy_attribute_type = ?";

    // private final String getEntitiesByName = "select ext.ext_id,
    // ext.entity_name, ext.ext_last_modified, ext.ext_deleted, ext.update_ts,
    // ext.state, dsy.dsy_variant_key, dsy.dsy_version, dsy.dsy_deleted from
    // sync_ext as ext join sync_dsy as dsy where ext.entity_name = ?";

    private final String getEntitiesByExtId = "select ext.ext_id, ext.entity_name, ext.ext_last_modified, ext.ext_deleted, ext.update_ts, ext.state, dsy.dsy_var_key, dsy.dsy_version, dsy.dsy_deleted, ext.language from sync_ext as ext join sync_dsy as dsy on ext.id = dsy.sync_id where ext.entity_name = ? and ext.ext_id = ? and ext.language = ?";

    private final String getEntitiesByState = "select ext.ext_id, ext.entity_name, ext.ext_last_modified, ext.ext_deleted, ext.update_ts, ext.state, dsy.dsy_var_key, dsy.dsy_version, dsy.dsy_deleted, ext.language from sync_ext as ext join sync_dsy as dsy on ext.id = dsy.sync_id where ext.state=?";

    private final String getEntityIdsByName = "select dsy.dsy_var_key from sync_ext as ext join sync_dsy as dsy on ext.id = dsy.sync_id where ext.entity_name = ?";

    private final String getEntityByVariantKey = "select ext.ext_id, ext.entity_name, ext.ext_last_modified, ext.ext_deleted, ext.update_ts, ext.state, dsy.dsy_var_key, dsy.dsy_version, dsy.dsy_deleted, ext.language from sync_ext as ext join sync_dsy as dsy on ext.id=dsy.sync_id where dsy.dsy_var_key = ?";

    private final String getAttributesByVariantKey = "select dsy_field_name, dsy_attribute_type, i, value from sync_value where dsy_var_key = ? ";

    private final String getEntitiesByDaisyDeleted = "select ext.ext_id, ext.entity_name, ext.ext_last_modified, ext.ext_deleted, ext.update_ts, ext.state, dsy.dsy_var_key, dsy.dsy_version, dsy.dsy_deleted, ext.language from sync_ext as ext join sync_dsy as dsy on ext.id=dsy.sync_id where dsy.dsy_deleted = ?";

    private final String checkExistence = "select count(*) from sync_dsy where dsy_var_key = ?";

    private SimpleJdbcTemplate jdbcTemplate;

    private TransactionTemplate transactionTemplate;
    
    private Logger logger = Logger.getLogger(this.getClass().getName());

    public SyncDBEntityDao(SimpleJdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    public void storeEntity(final Entity entity) {
        transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                String variantKey = VariantHelper.variantKeyToString(entity.getDaisyVariantKey());
                int rowCount = jdbcTemplate.queryForInt(checkExistence, new Object[] { variantKey });
                if (rowCount == 0) {
                    // Create new instance
                    insertSyncEntity(entity);
                    long syncId = jdbcTemplate.queryForLong(getLastId, new Object[] {});
                    insertDaisySyncEntity(entity, syncId);
                    for (Attribute attribute : entity.getAttributes()) {
                        insertAttribute(attribute, variantKey);
                    }

                } else {
                    // update instance
                    updateEntity(entity);
                    for (Attribute attribute : entity.getAttributes())
                        updateAttribute(attribute, variantKey);
                }
                return null;
            }
        });
    }

    public List<VariantKey> getEntityIds(String entityName) {
        return jdbcTemplate.query(getEntityIdsByName, new VariantKeyRowMapper(), new Object[] { entityName });
    }

    public Entity getEntity(VariantKey variantKey) throws EntityNotFoundException{
        Object[] args = new Object[] { VariantHelper.variantKeyToString(variantKey) };
        Entity entity = null;
        
        try {
            entity = jdbcTemplate.queryForObject(getEntityByVariantKey, new EntityRowMapper(), args);
        } catch (EmptyResultDataAccessException e) {
            // if there are no results just return null 
            throw new EntityNotFoundException("Could not find sync entity for variant key : " + variantKey, e);
        }
        
        if (entity != null)
            jdbcTemplate.query(getAttributesByVariantKey, new AttributeRowMapper(entity), args);

        return entity;
    }

    public Entity getEntity(String entityName, long externalId, String language) throws Exception {
        Entity entity = null;
        try {
            entity = jdbcTemplate.queryForObject(getEntitiesByExtId, new EntityRowMapper(), new Object[] { entityName, externalId, language });
        } catch (EmptyResultDataAccessException e) {
            // if there are no results just return null
        }
        if (entity != null)
            jdbcTemplate.query(getAttributesByVariantKey, new AttributeRowMapper(entity), new Object[] { VariantHelper.variantKeyToString(entity
                    .getDaisyVariantKey()) });

        return entity;
    }

    public List<Entity> getEntitiesByState(SyncState state) {
        List<Entity> list = jdbcTemplate.query(getEntitiesByState, new EntityRowMapper(), new Object[] { state.toString() });
        for (Entity entity : list) {
            jdbcTemplate.query(getAttributesByVariantKey, new AttributeRowMapper(entity), new Object[] { VariantHelper.variantKeyToString(entity
                    .getDaisyVariantKey()) });
        }
        return list;
    }

    public List<Entity> getDaisyDeletedEntities() {
        List<Entity> list = jdbcTemplate.query(getEntitiesByDaisyDeleted, new EntityRowMapper(), new Object[] { Boolean.TRUE });
        for (Entity entity : list) {
            jdbcTemplate.query(getAttributesByVariantKey, new AttributeRowMapper(entity), new Object[] { VariantHelper.variantKeyToString(entity
                    .getDaisyVariantKey()) });
        }
        return list;
    }

    public void replaceEntity(final VariantKey keyToBeReplaced, final Entity replacement) {
        final String oldEntityKey = VariantHelper.variantKeyToString(keyToBeReplaced);
        final String newEntityKey = VariantHelper.variantKeyToString(replacement.getDaisyVariantKey());
        transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                for (final Attribute attribute : replacement.getAttributes()) {
                    final PreparedStatementSetter attributeDeleter = new PreparedStatementSetter() {
                        public void setValues(PreparedStatement ps) throws SQLException {
                            ps.setString(1, oldEntityKey);
                            ps.setString(2, attribute.getDaisyName());
                            ps.setString(3, attribute.getType().toString());
                        }
                    };
                    jdbcTemplate.getJdbcOperations().update(deleteAttributes, attributeDeleter);
                }

                PreparedStatementSetter syncSetter = new PreparedStatementSetter() {
                    public void setValues(PreparedStatement ps) throws SQLException {
                        ps.setDate(1, replacement.getExternalLastModified() == null ? null : new Date(replacement.getExternalLastModified().getTime()));
                        ps.setBoolean(2, replacement.isExternalDeleted());
                        ps.setDate(3, new Date(replacement.getUpdateTimestamp().getTime()));
                        ps.setString(4, replacement.getState().toString());
                        ps.setLong(5, replacement.getDaisyVersion());
                        ps.setBoolean(6, replacement.isDaisyDeleted());
                        ps.setString(7, oldEntityKey);
                    }
                };
                jdbcTemplate.getJdbcOperations().update(updateSyncEntity, syncSetter);

                PreparedStatementSetter dsySyncSetter = new PreparedStatementSetter() {
                    public void setValues(PreparedStatement ps) throws SQLException {
                        ps.setString(1, newEntityKey);
                        ps.setLong(2, replacement.getDaisyVersion());
                        ps.setBoolean(3, replacement.isDaisyDeleted());
                        ps.setString(4, oldEntityKey);
                    }
                };
                jdbcTemplate.getJdbcOperations().update(replaceDaisySyncEntity, dsySyncSetter);

                for (final Attribute attribute : replacement.getAttributes()) {
                    insertAttribute(attribute, newEntityKey);
                }

                return null;
            }
        });

    };

    private void insertSyncEntity(final Entity entity) {
        PreparedStatementSetter syncSetter = new PreparedStatementSetter() {
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setLong(1, entity.getExternalId());
                ps.setString(2, entity.getInternalName());
                ps.setDate(3, entity.getExternalLastModified() == null ? null : new Date(entity.getExternalLastModified().getTime()));
                ps.setBoolean(4, entity.isExternalDeleted());
                ps.setDate(5, new Date(entity.getUpdateTimestamp().getTime()));
                ps.setString(6, entity.getState().toString());
                ps.setString(7, entity.getLanguage());
            }
        };
        jdbcTemplate.getJdbcOperations().update(insertExtSyncEntity, syncSetter);
    }

    private void insertDaisySyncEntity(final Entity entity, final long syncId) {
        final PreparedStatementSetter daisySyncSetter = new PreparedStatementSetter() {
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setLong(1, syncId);
                ps.setString(2, VariantHelper.variantKeyToString(entity.getDaisyVariantKey()));
                ps.setLong(3, entity.getDaisyVersion());
                ps.setBoolean(4, entity.isDaisyDeleted());
            }
        };
        jdbcTemplate.getJdbcOperations().update(insertDaisySyncEntity, daisySyncSetter);
    }

    private void insertAttribute(final Attribute attribute, final String variantKey) {
        if (attribute.getValues() != null) {
            for (int i = 0; i < attribute.getValues().size(); i++) {
                final int index = i;
                final String value = attribute.getValues().get(i);
                PreparedStatementSetter attributeSetter = new PreparedStatementSetter() {
                    public void setValues(PreparedStatement ps) throws SQLException {
                        ps.setString(1, variantKey);
                        ps.setString(2, attribute.getDaisyName());
                        ps.setString(3, attribute.getType().toString());
                        ps.setInt(4, index);
                        ps.setString(5, value);
                    }
                };
                jdbcTemplate.getJdbcOperations().update(insertAttribute, attributeSetter);
            }
        }
    }

    private void updateEntity(final Entity entity) {
        PreparedStatementSetter syncSetter = new PreparedStatementSetter() {
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setDate(1, entity.getExternalLastModified() == null ? null : new Date(entity.getExternalLastModified().getTime()));
                ps.setBoolean(2, entity.isExternalDeleted());
                ps.setDate(3, new Date(entity.getUpdateTimestamp().getTime()));
                ps.setString(4, entity.getState().toString());
                ps.setLong(5, entity.getDaisyVersion());
                ps.setBoolean(6, entity.isDaisyDeleted());
                ps.setString(7, VariantHelper.variantKeyToString(entity.getDaisyVariantKey()));
            }
        };
        jdbcTemplate.getJdbcOperations().update(updateSyncEntity, syncSetter);
    }

    private void updateAttribute(final Attribute attribute, final String variantKey) {

        final PreparedStatementSetter attributeDeleter = new PreparedStatementSetter() {
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setString(1, variantKey);
                ps.setString(2, attribute.getDaisyName());
                ps.setString(3, attribute.getType().toString());
            }
        };
        jdbcTemplate.getJdbcOperations().update(deleteAttributes, attributeDeleter);

        insertAttribute(attribute, variantKey);
    }

    private class EntityRowMapper implements ParameterizedRowMapper<Entity> {
        public Entity mapRow(ResultSet rs, int rowNum) throws SQLException {
            Entity entity = new EntityImpl();
            entity.setExternalId(rs.getLong("ext.ext_id"));
            entity.setInternalName(rs.getString("ext.entity_name"));
            entity.setExternalLastModified(rs.getDate("ext.ext_last_modified"));
            entity.setExternalDeleted(rs.getBoolean("ext.ext_deleted"));
            entity.setUpdateTimestamp(rs.getDate("ext.update_ts"));
            entity.setState(SyncState.valueOf(rs.getString("ext.state")));
            entity.setDaisyVariantKey(VariantHelper.extractVariantKey(rs.getString("dsy.dsy_var_key")));
            entity.setDaisyVersion(rs.getLong("dsy.dsy_version"));
            entity.setDaisyDeleted(rs.getBoolean("dsy.dsy_deleted"));
            entity.setLanguage(rs.getString("ext.language"));
            return entity;
        }
    }

    private class AttributeRowMapper implements ParameterizedRowMapper<Attribute> {
        public Entity entity;

        public AttributeRowMapper(Entity entity) {
            this.entity = entity;
        }

        public Attribute mapRow(ResultSet rs, int rowNum) throws SQLException {
            int index = rs.getInt("i");
            Attribute attribute = null;
            if (index == 0) {
                attribute = new AttributeImpl();
                attribute.setDaisyName(rs.getString("dsy_field_name"));
                try {
                    attribute.setType(AttributeType.valueOf(rs.getString("dsy_attribute_type")));
                } catch (Exception e) {
                    throw new SQLException("Could not set attribute type for attribute " + attribute.toString());
                }
                this.entity.addAttribute(attribute);
            } else {
                attribute = entity.getAttributeByDaisyName(rs.getString("dsy_field_name"), AttributeType.valueOf(rs.getString("dsy_attribute_type")));
            }
            attribute.addValue(rs.getString("value"));
            return attribute;
        }
    }

    private class VariantKeyRowMapper implements ParameterizedRowMapper<VariantKey> {

        public VariantKey mapRow(ResultSet rs, int rowNum) throws SQLException {
            return VariantHelper.extractVariantKey(rs.getString("dsy.dsy_var_key"));
        }

    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

}
