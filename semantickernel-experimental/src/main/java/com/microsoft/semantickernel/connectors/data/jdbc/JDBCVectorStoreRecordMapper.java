// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.connectors.data.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.semantickernel.builders.SemanticKernelBuilder;
import com.microsoft.semantickernel.exceptions.SKException;
import com.microsoft.semantickernel.data.VectorStoreRecordMapper;
import com.microsoft.semantickernel.data.recorddefinition.VectorStoreRecordDefinition;
import com.microsoft.semantickernel.data.recorddefinition.VectorStoreRecordField;
import com.microsoft.semantickernel.data.recorddefinition.VectorStoreRecordVectorField;

import java.sql.ResultSetMetaData;
import java.util.List;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

public class JDBCVectorStoreRecordMapper<Record>
    extends VectorStoreRecordMapper<Record, ResultSet> {

    /**
     * Constructs a new instance of the VectorStoreRecordMapper.
     *
     * @param storageModelToRecordMapper the function to convert a storage model to a record
     */
    protected JDBCVectorStoreRecordMapper(Function<ResultSet, Record> storageModelToRecordMapper) {
        super(null, storageModelToRecordMapper);
    }

    /**
     * Creates a new builder.
     *
     * @param <Record> the record type
     * @return the builder
     */
    public static <Record> Builder<Record> builder() {
        return new Builder<>();
    }

    /**
     * Operation not supported.
     */
    @Override
    public ResultSet mapRecordToStorageModel(Record record) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public static class Builder<Record>
        implements SemanticKernelBuilder<JDBCVectorStoreRecordMapper<Record>> {
        private Class<Record> recordClass;
        private VectorStoreRecordDefinition vectorStoreRecordDefinition;

        /**
         * Sets the record class.
         *
         * @param recordClass the record class
         * @return the builder
         */
        public Builder<Record> withRecordClass(Class<Record> recordClass) {
            this.recordClass = recordClass;
            return this;
        }

        /**
         * Sets the vector store record definition.
         *
         * @param vectorStoreRecordDefinition the vector store record definition
         * @return the builder
         */
        public Builder<Record> withVectorStoreRecordDefinition(
            VectorStoreRecordDefinition vectorStoreRecordDefinition) {
            this.vectorStoreRecordDefinition = vectorStoreRecordDefinition;
            return this;
        }

        /**
         * Builds the {@link JDBCVectorStoreRecordMapper}.
         *
         * @return the {@link JDBCVectorStoreRecordMapper}
         */
        public JDBCVectorStoreRecordMapper<Record> build() {
            if (recordClass == null) {
                throw new IllegalArgumentException("recordClass is required");
            }
            if (vectorStoreRecordDefinition == null) {
                throw new IllegalArgumentException("vectorStoreRecordDefinition is required");
            }

            return new JDBCVectorStoreRecordMapper<>(
                resultSet -> {
                    try {
                        Constructor<?> constructor = recordClass.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        Record record = (Record) constructor.newInstance();

                        // Select fields from the record definition.
                        // Check if vector fields are present in the result set.
                        List<VectorStoreRecordField> fields;
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        if (metaData.getColumnCount() == vectorStoreRecordDefinition.getAllFields()
                            .size()) {
                            fields = vectorStoreRecordDefinition.getAllFields();
                        } else {
                            fields = vectorStoreRecordDefinition.getNonVectorFields();
                        }

                        for (VectorStoreRecordField field : fields) {
                            Object value = resultSet.getObject(field.getName());
                            Field recordField = recordClass.getDeclaredField(field.getName());
                            recordField.setAccessible(true);

                            // If the field is a vector field, deserialize the JSON string
                            if (field instanceof VectorStoreRecordVectorField) {
                                Class<?> vectorType = recordField.getType();

                                // If the vector type is a string, set the value directly
                                if (vectorType.equals(String.class)) {
                                    recordField.set(record, value);
                                } else {
                                    // Deserialize the JSON string to the vector type
                                    recordField.set(record,
                                        new ObjectMapper().readValue((String) value, vectorType));
                                }
                            } else {
                                recordField.set(record, value);
                            }
                        }

                        return record;
                    } catch (NoSuchMethodException e) {
                        throw new SKException("Default constructor not found.", e);
                    } catch (InstantiationException | InvocationTargetException e) {
                        throw new SKException(String.format(
                            "SK cannot instantiate %s. A custom mapper is required.",
                            recordClass.getName()), e);
                    } catch (JsonProcessingException e) {
                        throw new SKException(String.format(
                            "SK cannot deserialize %s. A custom mapper is required.",
                            recordClass.getName()), e);
                    } catch (SQLException | NoSuchFieldException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
        }
    }
}
