package org.kie.server.api.marshalling;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.Test;
import org.kie.server.api.marshalling.objects.DateObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JSONMarshallerTest {

    @Test
    public void testMarshallDateObject() {
        String expectedString = "{\n" +
                "  \"localDate\" : \"2017-01-01\",\n" +
                "  \"localDateTime\" : \"2017-01-01T10:10:10\",\n" +
                "  \"localTime\" : \"10:10:10\",\n" +
                "  \"offsetDateTime\" : \"2017-01-01T10:10:10+01:00\"\n" +
                "}";

        Marshaller marshaller = MarshallerFactory.getMarshaller( MarshallingFormat.JSON, getClass().getClassLoader() );

        DateObject dateObject = new DateObject();
        dateObject.setLocalDate( LocalDate.of( 2017, 1, 1 ) );
        dateObject.setLocalDateTime( LocalDateTime.of( 2017, 1, 1, 10, 10, 10 ) );
        dateObject.setLocalTime( LocalTime.of( 10, 10, 10 ) );
        dateObject.setOffsetDateTime( OffsetDateTime.of( LocalDateTime.of( 2017, 1, 1, 10, 10, 10 ), ZoneOffset.ofHours( 1 ) ) );

        String dateObjectString = marshaller.marshall( dateObject );
        assertNotNull( dateObjectString );

        assertEquals( expectedString, dateObjectString );
    }

    @Test
    public void testUnmarshallDateObject() {
        String expectedString = "{\n" +
                "  \"localDate\" : \"2017-01-01\",\n" +
                "  \"localDateTime\" : \"2017-01-01T10:10:10\",\n" +
                "  \"localTime\" : \"10:10:10\",\n" +
                "  \"offsetDateTime\" : \"2017-01-01T10:10:10+01:00\"\n" +
                "}";

        Marshaller marshaller = MarshallerFactory.getMarshaller( MarshallingFormat.JSON, getClass().getClassLoader() );

        DateObject dateObject = marshaller.unmarshall( expectedString, DateObject.class );
        assertNotNull( dateObject );

        assertEquals( LocalDate.of( 2017, 1, 1 ), dateObject.getLocalDate() );
        assertEquals( LocalDateTime.of( 2017, 1, 1, 10, 10, 10 ), dateObject.getLocalDateTime() );
        assertEquals( LocalTime.of( 10, 10, 10 ), dateObject.getLocalTime() );
        assertEquals( OffsetDateTime.of( LocalDateTime.of( 2017, 1, 1, 10, 10, 10 ), ZoneOffset.ofHours( 1 ) ), dateObject.getOffsetDateTime() );
    }

}
