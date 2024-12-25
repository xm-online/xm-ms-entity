package com.icthh.xm.lep


import groovy.transform.ToString
import org.junit.Test

import java.time.LocalDate

import static org.junit.Assert.assertArrayEquals
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

@ToString(includeNames = true, ignoreNulls = false)
class LepDataClassTransformationUnitTest {

    enum Country {
        USA, UK, CANADA
    }

    enum Status {
        ACTIVE, INACTIVE
    }

    @Test
    public void testLepDataClass() throws Exception {
        GroovyClassLoader loader = new GroovyClassLoader(getClass().getClassLoader());

        // language=groovy
        String script = """
        import com.icthh.xm.ms.entity.lep.transform.LepDataClass
        import com.icthh.xm.ms.entity.lep.transform.LepDataClassField
        import java.time.LocalDate
        import groovy.transform.ToString
        import com.icthh.xm.lep.LepDataClassTransformationUnitTest.Country
        import com.icthh.xm.lep.LepDataClassTransformationUnitTest.Status

        @LepDataClass
        @ToString(includeNames = true, ignoreNulls = false)
        class Person {
            public String name;
            @LepDataClassField(Address.class)
            public Collection addresses;
            public Collection<Address> addressesWithoutAnnotation;
            public LocalDate birthDate;
            public Status status;
            public UnannotatedSomeClass anyField;
            public AnnotatedSomeClass otherField;
            Map<String, Address> addressMap;
            Map<String, String> stringMap;
            Map<Address, String> addressStringMap;

            int[] numbers;

            Address[] addressArray;
        }

        @LepDataClass
        class Address {
            public String street;
            public String city;
            public Country country;
        }

        @LepDataClass
        class AnnotatedSomeClass {
            public Status status;
        }

        class UnannotatedSomeClass {
            public String title;
        }


        """

        def inputMap = [
            name     : 'John',
            addressStringMap: [
                [street: 'Main St', city: 'NY', country: 'USA']: 'USA',
                [street: 'Baker St', city: 'London', country: 'UK']: 'UK'
            ],
            stringMap: [
                'NY': 'USA',
                'London': 'UK'
            ],
            addressMap: [
                'NY': [street: 'Main St', city: 'NY', country: 'USA'],
                'London': [street: 'Baker St', city: 'London', country: 'UK']
            ],
            addressesWithoutAnnotation: [
                [street: 'Main St', city: 'NY', country: 'USA', ignoredField: 'ignored'],
                [street: 'Baker St', city: 'London', country: 'UK']
            ],
            addresses: [
                [street: 'Main St', city: 'NY', country: 'USA', ignoredField: 'ignored'],
                [street: 'Baker St', city: 'London', country: 'UK']
            ],
            addressArray: [
                [street: 'Main St', city: 'NY', country: 'USA', ignoredField: 'ignored'],
                [street: 'Baker St', city: 'London', country: 'UK']
            ],
            birthDate: '2000-01-01',
            status   : 'ACTIVE',
            anyField : [title: 'Casted'],
            otherField : [status: 'INACTIVE'],
            numbers: [1, 2, 3, 4, 5]
        ];

         def person = loader.parseClass(script, "Person.groovy")
            .getConstructor(Map.class)
            .newInstance(inputMap);

        // Basic null-check
        assertNotNull(person);

        // name
        assertEquals("John", person.name);

        // addresses
        assertNotNull(person.addresses);
        assertTwoAddressCollection(person.addresses);

        // addressesWithoutAnnotation
        assertNotNull(person.addressesWithoutAnnotation);
        assertTwoAddressCollection(person.addressesWithoutAnnotation);

        // birthDate
        assertNotNull(person.birthDate);
        assertEquals(LocalDate.of(2000, 1, 1), person.birthDate);

        // status
        assertNotNull(person.status);
        assertEquals(Status.ACTIVE, person.status);

        // anyField (UnannotatedSomeClass)
        assertNotNull(person.anyField);
        assertEquals("Casted", person.anyField.title);

        // otherField (AnnotatedSomeClass)
        assertNotNull(person.otherField);
        assertNotNull(person.otherField.status);
        assertEquals(Status.INACTIVE, person.otherField.status);

        // addressMap
        assertNotNull(person.addressMap);
        assertAddressMap(person.addressMap);

        // stringMap
        assertNotNull(person.stringMap);
        assertStringMap(person.stringMap);

        // addressStringMap
        assertNotNull(person.addressStringMap);
        assertAddressStringMap(person.addressStringMap);

        // numbers
        assertNotNull(person.numbers);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, person.numbers);

        // addressArray
        assertNotNull(person.addressArray);
        assertTwoAddressArray(person.addressArray);
    }

    private void assertTwoAddressCollection(Collection<Object> addresses) {
        assertEquals(2, addresses.size());
        Object[] arr = addresses.toArray(new Object[0]);
        assertNYUSAAddress(arr[0]);
        assertBakerLondonAddress(arr[1]);
    }

    private void assertTwoAddressArray(Object[] addresses) {
        assertEquals(2, addresses.length);
        assertNYUSAAddress(addresses[0]);
        assertBakerLondonAddress(addresses[1]);
    }

    private void assertNYUSAAddress(Object address) {
        assertEquals("Main St", address.street);
        assertEquals("NY", address.city);
        assertEquals(Country.USA, address.country);
    }

    private void assertBakerLondonAddress(Object address) {
        assertEquals("Baker St", address.street);
        assertEquals("London", address.city);
        assertEquals(Country.UK, address.country);
    }

    private void assertAddressMap(Map<String, Object> addressMap) {
        assertEquals(2, addressMap.size());

        Object nyAddr = addressMap.get("NY");
        assertNotNull(nyAddr);
        assertNYUSAAddress(nyAddr);

        Object londonAddr = addressMap.get("London");
        assertNotNull(londonAddr);
        assertBakerLondonAddress(londonAddr);
    }

    private void assertStringMap(Map<String, String> stringMap) {
        assertEquals(2, stringMap.size());
        assertEquals("USA", stringMap.get("NY"));
        assertEquals("UK", stringMap.get("London"));
    }

    private void assertAddressStringMap(Map<Object, String> addressStringMap) {
        assertEquals(2, addressStringMap.size());

        boolean foundMainStKey = false;
        boolean foundBakerStKey = false;

        for (Map.Entry<Object, String> entry : addressStringMap.entrySet()) {
            Object addrKey = entry.getKey();
            String value = entry.getValue();

            if ("Main St".equals(addrKey.street)
                && "NY".equals(addrKey.city)
                && Country.USA.equals(addrKey.country)) {
                foundMainStKey = true;
                assertEquals("USA", value);
            } else if ("Baker St".equals(addrKey.street)
                && "London".equals(addrKey.city)
                && Country.UK.equals(addrKey.country)) {
                foundBakerStKey = true;
                assertEquals("UK", value);
            }
        }

        assertTrue(foundMainStKey);
        assertTrue(foundBakerStKey);
    }

}
