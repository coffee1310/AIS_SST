package org.example.ais_sst.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.ais_sst.entity.enums.TaskRequestStatus;

@Converter(autoApply = true)
public class TaskRequestStatusConverter implements AttributeConverter<TaskRequestStatus, String> {

    @Override
    public String convertToDatabaseColumn(TaskRequestStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDisplayName();
    }

    @Override
    public TaskRequestStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return TaskRequestStatus.fromDisplayName(dbData);
    }
}