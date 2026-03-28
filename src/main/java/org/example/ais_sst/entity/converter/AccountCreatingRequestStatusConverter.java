package org.example.ais_sst.entity.converter;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.ais_sst.entity.enums.AccountCreatingRequestStatus;

@Converter(autoApply = true)
public class AccountCreatingRequestStatusConverter
        implements AttributeConverter<AccountCreatingRequestStatus, String> {

    @Override
    public String convertToDatabaseColumn(AccountCreatingRequestStatus status) {
        if (status == null) {
            return null;
        }
        switch (status) {
            case НА_РАССМОТРЕНИИ:
                return "На рассмотрении";
            case ОДОБРЕНА:
                return "Одобрена";
            case ОТКЛОНЕНА:
                return "Отклонена";
            default:
                throw new IllegalArgumentException("Unknown status: " + status);
        }
    }

    @Override
    public AccountCreatingRequestStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        switch (dbData) {
            case "На рассмотрении":
                return AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ;
            case "Одобрена":
                return AccountCreatingRequestStatus.ОДОБРЕНА;
            case "Отклонена":
                return AccountCreatingRequestStatus.ОТКЛОНЕНА;
            default:
                throw new IllegalArgumentException("Unknown DB value: " + dbData);
        }
    }
}