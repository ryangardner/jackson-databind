package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

// Tests for [JACKSON-453]
public class TestExternalId extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */
    
    static class ExternalBean
    {
        @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType")
        public Object bean;

        public ExternalBean() { }
        public ExternalBean(int v) {
            bean = new ValueBean(v);
        }
    }

    static class ExternalBean3
    {
        @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType1")
        public Object value1;
        
        @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType2")
        public Object value2;

        public int foo;
        
        @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType3")
        public Object value3;
        
        public ExternalBean3() { }
        public ExternalBean3(int v) {
            value1 = new ValueBean(v);
            value2 = new ValueBean(v+1);
            value3 = new ValueBean(v+2);
            foo = v;
        }
    }

    static class ExternalBeanWithCreator
    {
        @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType")
        public Object value;

        public int foo;
        
        @JsonCreator
        public ExternalBeanWithCreator(@JsonProperty("foo") int f)
        {
            foo = f;
            value = new ValueBean(f);
        }
    }
    
    @JsonTypeName("vbean")
    static class ValueBean {
        public int value;
        
        public ValueBean() { }
        public ValueBean(int v) { value = v; }
    }

    @JsonTypeName("funk")
    @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType")
    static class FunkyExternalBean {
        public int i = 3;
    }

    // [JACKSON-798]: problems with polymorphic types, external prop

    @JsonSubTypes(value= { @JsonSubTypes.Type(value=Derived1.class, name="d1"),
            @JsonSubTypes.Type(value=Derived2.class, name="d2") })
    interface Base {
        String getBaseProperty();
    }
  
    static class Derived1 implements Base {
        private String derived1Property;
        private String baseProperty;
        protected  Derived1() { throw new IllegalStateException("wrong constructor called"); }
        
        @JsonCreator
        public Derived1(@JsonProperty("derived1Property") String d1p,
                        @JsonProperty("baseProperty") String bp) {
            derived1Property = d1p;
            baseProperty = bp;
        }

        @Override
        @JsonProperty public String getBaseProperty() {
            return baseProperty;
        }

        @JsonProperty public String getDerived1Property() {
            return derived1Property;
        }
    }

    static class Derived2 implements Base {
        private String derived2Property;
        private String baseProperty;
        protected  Derived2() { throw new IllegalStateException("wrong constructor called"); }

        @JsonCreator
        public Derived2(@JsonProperty("derived2Property") String d2p,
                        @JsonProperty("baseProperty") String bp) {
            derived2Property = d2p;
            baseProperty = bp;
        }

        @Override
        @JsonProperty public String getBaseProperty() {
            return baseProperty;
        }

        @JsonProperty public String getDerived2Property() {
            return derived2Property;
        }
    }
    
    static class BaseContainer {
        protected final Base base;
        protected final String baseContainerProperty;
        protected BaseContainer() { throw new IllegalStateException("wrong constructor called"); }

        @JsonCreator
        public BaseContainer(@JsonProperty("baseContainerProperty") String bcp, @JsonProperty("base") Base b) {
            baseContainerProperty = bcp;
            base = b;
        }

        @JsonProperty
        public String getBaseContainerProperty() { return baseContainerProperty; }

        @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.EXTERNAL_PROPERTY, property="type")
        @JsonProperty
        public Base getBase() { return base; }
    }
    
    /*
    /**********************************************************
    /* Unit tests, serialization
    /**********************************************************
     */
    
    public void testSimpleSerialization() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(ValueBean.class);
        // This may look odd, but one implementation nastiness is the fact
        // that we can not properly serialize type id before the object,
        // because call is made after property name (for object) has already
        // been written out. So we'll write it after...
        // Deserializer will work either way as it can not rely on ordering
        // anyway.
        assertEquals("{\"bean\":{\"value\":11},\"extType\":\"vbean\"}",
                mapper.writeValueAsString(new ExternalBean(11)));
    }

    // If trying to use with Class, should just become "PROPERTY" instead:
    public void testImproperExternalIdSerialization() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("{\"extType\":\"funk\",\"i\":3}",
                mapper.writeValueAsString(new FunkyExternalBean()));
    }

    /*
    /**********************************************************
    /* Unit tests, deserialization
    /**********************************************************
     */

    public void testSimpleDeserialization() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(ValueBean.class);
        ExternalBean result = mapper.readValue("{\"bean\":{\"value\":11},\"extType\":\"vbean\"}", ExternalBean.class);
        assertNotNull(result);
        assertNotNull(result.bean);
        ValueBean vb = (ValueBean) result.bean;
        assertEquals(11, vb.value);
    }

    // Test for verifying that it's ok to have multiple (say, 3)
    // externally typed things, mixed with other stuff...
    public void testMultipleTypeIdsDeserialization() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(ValueBean.class);
        String json = mapper.writeValueAsString(new ExternalBean3(3));
        ExternalBean3 result = mapper.readValue(json, ExternalBean3.class);
        assertNotNull(result);
        assertNotNull(result.value1);
        assertNotNull(result.value2);
        assertNotNull(result.value3);
        assertEquals(3, ((ValueBean)result.value1).value);
        assertEquals(4, ((ValueBean)result.value2).value);
        assertEquals(5, ((ValueBean)result.value3).value);
        assertEquals(3, result.foo);
    }

    // Also, it should be ok to use @JsonCreator as well...
    public void testExternalTypeWithCreator() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(ValueBean.class);
        String json = mapper.writeValueAsString(new ExternalBeanWithCreator(7));
        ExternalBeanWithCreator result = mapper.readValue(json, ExternalBeanWithCreator.class);
        assertNotNull(result);
        assertNotNull(result.value);
        assertEquals(7, ((ValueBean)result.value).value);
        assertEquals(7, result.foo);
    }
    
    // If trying to use with Class, should just become "PROPERTY" instead:
    public void testImproperExternalIdDeserialization() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        FunkyExternalBean result = mapper.readValue("{\"extType\":\"funk\",\"i\":3}", FunkyExternalBean.class);
        assertNotNull(result);
        assertEquals(3, result.i);
    }

    public void testIssue798() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        Base base = new Derived1("derived1 prop val", "base prop val");
        BaseContainer baseContainer = new BaseContainer("bc prop val", base);
        String generatedJson = mapper.writeValueAsString(baseContainer);
        BaseContainer baseContainer2 = mapper.readValue(generatedJson,BaseContainer.class);
        assertEquals("bc prop val", baseContainer.getBaseContainerProperty());

        Base b = baseContainer2.getBase();
        assertNotNull(b);
        if (b.getClass() != Derived1.class) {
            fail("Should have type Derived1, was "+b.getClass().getName());
        }

        Derived1 derived1 = (Derived1) b;
        assertEquals("base prop val", derived1.getBaseProperty());
        assertEquals("derived1 prop val", derived1.getDerived1Property());
    }
}
