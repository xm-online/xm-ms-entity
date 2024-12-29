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
        import com.icthh.xm.ms.entity.lep.transform.LepDataClassIgnored

        import java.time.LocalDate
        import groovy.transform.ToString
        import com.icthh.xm.lep.LepDataClassTransformationUnitTest.Country
        import com.icthh.xm.lep.LepDataClassTransformationUnitTest.Status

        @LepDataClass
        @ToString(includeNames = true, ignoreNulls = false)
        class Person {
            public String name;
            public Collection<Address> addresses;
            public LocalDate birthDate;
            public Status status;
            public UnannotatedSomeClass anyField;
            public AnnotatedSomeClass otherField;
            Map<String, Address> addressMap;
            Map<String, String> stringMap;
            Map<Address, String> addressStringMap;
            int[] numbers;
            Address[] addressArray;

            String defaultValue = "someDefaultValue";
            @LepDataClassIgnored
            String skippedValue = "skippedValue";

            Status nullEnum;
            LocalDate nullDate;
            Address[] nullArray;
            Collection<Address> nullCollection;
            int[] nullIntArray;

            public Collection<Collection<Collection<Address>>> innerCollections;
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
            skippedValue: 'NOT_SKIPPED',
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
            numbers: [1, 2, 3, 4, 5],
            innerCollections: [
                [
                    [
                        [street: 'Main St1', city: 'NY', country: 'USA'],
                        [street: 'Baker St2', city: 'London', country: 'UK']
                    ],
                    [
                        [street: 'Main St3', city: 'NY', country: 'USA'],
                        [street: 'Baker St4', city: 'London', country: 'UK']
                    ]
                ],
                [
                    [
                        [street: 'Main St5', city: 'NY', country: 'USA'],
                        [street: 'Baker St6', city: 'London', country: 'UK']
                    ],
                    [
                        [street: 'Main St7', city: 'NY', country: 'USA'],
                        [street: 'Baker St8', city: 'London', country: 'UK']
                    ]
                ]
            ]
        ];


        def scriptValue = loader.parseClass(script, "Person.groovy")
        scriptValue.getConstructor().newInstance();

        // check no NPE when map is null
        loader.parseClass("class Test { Test() { Map map = null; def p = new Person(map); println p;} }", "Test.groovy")
            .getConstructor().newInstance();

        def person = scriptValue.getConstructor(Map.class).newInstance(inputMap);
        assertEquals("someDefaultValue", person.defaultValue);
        assertEquals("skippedValue", person.skippedValue);
        assertEquals("John", person.name);
        assertTwoAddressCollection(person.addresses);
        assertEquals(LocalDate.of(2000, 1, 1), person.birthDate);
        assertEquals(Status.ACTIVE, person.status);
        assertEquals("Casted", person.anyField.title);
        assertEquals(Status.INACTIVE, person.otherField.status);
        assertAddressMap(person.addressMap);
        assertStringMap(person.stringMap);
        assertAddressStringMap(person.addressStringMap);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, person.numbers);
        assertTwoAddressArray(person.addressArray);

        def expected = inputMap
        expected.remove('skippedValue')
        expected.remove('anyField')
        expected.defaultValue = "someDefaultValue";
        expected.get('addresses').get(0).remove('ignoredField');
        expected.get('addressArray').get(0).remove('ignoredField');
        def actual = person.toMap()
        actual.remove('anyField')
        assertEquals(expected.remove("numbers").toList(), actual.remove("numbers").toList());
        assertEquals(expected, actual);
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
