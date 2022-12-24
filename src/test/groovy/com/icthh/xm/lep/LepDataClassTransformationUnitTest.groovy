package com.icthh.xm.lep


import com.icthh.xm.ms.entity.lep.helpers.LepDataClass
import org.junit.Test

class LepDataClassTransformationUnitTest {

    @Test
    public void test() {
        Map data = [a: 1, b: 2, c: 3]

        MyClass typedClassWithField = data
        assert typedClassWithField.a == 1
        assert typedClassWithField.b == 2
        assert typedClassWithField.c == 3

        typedClassWithField.b = 100
        assert data.b == 100

        Map mapFromClass = typedClassWithField
        mapFromClass.b = 200
        assert data.b == 200

        assert mapFromClass instanceof MyClass
        assert mapFromClass instanceof Map

        assert mapFromClass.a == 1
        assert mapFromClass.b != 200
        assert mapFromClass.c == 3
    }

    @LepDataClass
    public static class MyClass {
        int a = 0;
        int b = 0;

        static MyClass of(Map<String, Object> data) {
            MyClass instance = data;
            instance.__data__ = data;
            return instance
        }
    }

}
