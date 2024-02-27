/**
 * Autogenerated by Thrift Compiler (0.19.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.jetbrains.cef.remote.thrift_codegen;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
public class PostData implements org.apache.thrift.TBase<PostData, PostData._Fields>, java.io.Serializable, Cloneable, Comparable<PostData> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("PostData");

  private static final org.apache.thrift.protocol.TField IS_READ_ONLY_FIELD_DESC = new org.apache.thrift.protocol.TField("isReadOnly", org.apache.thrift.protocol.TType.BOOL, (short)1);
  private static final org.apache.thrift.protocol.TField HAS_EXCLUDED_ELEMENTS_FIELD_DESC = new org.apache.thrift.protocol.TField("hasExcludedElements", org.apache.thrift.protocol.TType.BOOL, (short)2);
  private static final org.apache.thrift.protocol.TField ELEMENTS_FIELD_DESC = new org.apache.thrift.protocol.TField("elements", org.apache.thrift.protocol.TType.LIST, (short)3);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new PostDataStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new PostDataTupleSchemeFactory();

  public boolean isReadOnly; // required
  public boolean hasExcludedElements; // required
  public @org.apache.thrift.annotation.Nullable java.util.List<PostDataElement> elements; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    IS_READ_ONLY((short)1, "isReadOnly"),
    HAS_EXCLUDED_ELEMENTS((short)2, "hasExcludedElements"),
    ELEMENTS((short)3, "elements");

    private static final java.util.Map<java.lang.String, _Fields> byName = new java.util.HashMap<java.lang.String, _Fields>();

    static {
      for (_Fields field : java.util.EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    @org.apache.thrift.annotation.Nullable
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // IS_READ_ONLY
          return IS_READ_ONLY;
        case 2: // HAS_EXCLUDED_ELEMENTS
          return HAS_EXCLUDED_ELEMENTS;
        case 3: // ELEMENTS
          return ELEMENTS;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new java.lang.IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    @org.apache.thrift.annotation.Nullable
    public static _Fields findByName(java.lang.String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final java.lang.String _fieldName;

    _Fields(short thriftId, java.lang.String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    @Override
    public short getThriftFieldId() {
      return _thriftId;
    }

    @Override
    public java.lang.String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final int __ISREADONLY_ISSET_ID = 0;
  private static final int __HASEXCLUDEDELEMENTS_ISSET_ID = 1;
  private byte __isset_bitfield = 0;
  private static final _Fields optionals[] = {_Fields.ELEMENTS};
  public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.IS_READ_ONLY, new org.apache.thrift.meta_data.FieldMetaData("isReadOnly", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
    tmpMap.put(_Fields.HAS_EXCLUDED_ELEMENTS, new org.apache.thrift.meta_data.FieldMetaData("hasExcludedElements", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
    tmpMap.put(_Fields.ELEMENTS, new org.apache.thrift.meta_data.FieldMetaData("elements", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, PostDataElement.class))));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(PostData.class, metaDataMap);
  }

  public PostData() {
  }

  public PostData(
    boolean isReadOnly,
    boolean hasExcludedElements)
  {
    this();
    this.isReadOnly = isReadOnly;
    setIsReadOnlyIsSet(true);
    this.hasExcludedElements = hasExcludedElements;
    setHasExcludedElementsIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public PostData(PostData other) {
    __isset_bitfield = other.__isset_bitfield;
    this.isReadOnly = other.isReadOnly;
    this.hasExcludedElements = other.hasExcludedElements;
    if (other.isSetElements()) {
      java.util.List<PostDataElement> __this__elements = new java.util.ArrayList<PostDataElement>(other.elements.size());
      for (PostDataElement other_element : other.elements) {
        __this__elements.add(new PostDataElement(other_element));
      }
      this.elements = __this__elements;
    }
  }

  @Override
  public PostData deepCopy() {
    return new PostData(this);
  }

  @Override
  public void clear() {
    setIsReadOnlyIsSet(false);
    this.isReadOnly = false;
    setHasExcludedElementsIsSet(false);
    this.hasExcludedElements = false;
    this.elements = null;
  }

  public boolean isIsReadOnly() {
    return this.isReadOnly;
  }

  public PostData setIsReadOnly(boolean isReadOnly) {
    this.isReadOnly = isReadOnly;
    setIsReadOnlyIsSet(true);
    return this;
  }

  public void unsetIsReadOnly() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __ISREADONLY_ISSET_ID);
  }

  /** Returns true if field isReadOnly is set (has been assigned a value) and false otherwise */
  public boolean isSetIsReadOnly() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __ISREADONLY_ISSET_ID);
  }

  public void setIsReadOnlyIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __ISREADONLY_ISSET_ID, value);
  }

  public boolean isHasExcludedElements() {
    return this.hasExcludedElements;
  }

  public PostData setHasExcludedElements(boolean hasExcludedElements) {
    this.hasExcludedElements = hasExcludedElements;
    setHasExcludedElementsIsSet(true);
    return this;
  }

  public void unsetHasExcludedElements() {
    __isset_bitfield = org.apache.thrift.EncodingUtils.clearBit(__isset_bitfield, __HASEXCLUDEDELEMENTS_ISSET_ID);
  }

  /** Returns true if field hasExcludedElements is set (has been assigned a value) and false otherwise */
  public boolean isSetHasExcludedElements() {
    return org.apache.thrift.EncodingUtils.testBit(__isset_bitfield, __HASEXCLUDEDELEMENTS_ISSET_ID);
  }

  public void setHasExcludedElementsIsSet(boolean value) {
    __isset_bitfield = org.apache.thrift.EncodingUtils.setBit(__isset_bitfield, __HASEXCLUDEDELEMENTS_ISSET_ID, value);
  }

  public int getElementsSize() {
    return (this.elements == null) ? 0 : this.elements.size();
  }

  @org.apache.thrift.annotation.Nullable
  public java.util.Iterator<PostDataElement> getElementsIterator() {
    return (this.elements == null) ? null : this.elements.iterator();
  }

  public void addToElements(PostDataElement elem) {
    if (this.elements == null) {
      this.elements = new java.util.ArrayList<PostDataElement>();
    }
    this.elements.add(elem);
  }

  @org.apache.thrift.annotation.Nullable
  public java.util.List<PostDataElement> getElements() {
    return this.elements;
  }

  public PostData setElements(@org.apache.thrift.annotation.Nullable java.util.List<PostDataElement> elements) {
    this.elements = elements;
    return this;
  }

  public void unsetElements() {
    this.elements = null;
  }

  /** Returns true if field elements is set (has been assigned a value) and false otherwise */
  public boolean isSetElements() {
    return this.elements != null;
  }

  public void setElementsIsSet(boolean value) {
    if (!value) {
      this.elements = null;
    }
  }

  @Override
  public void setFieldValue(_Fields field, @org.apache.thrift.annotation.Nullable java.lang.Object value) {
    switch (field) {
    case IS_READ_ONLY:
      if (value == null) {
        unsetIsReadOnly();
      } else {
        setIsReadOnly((java.lang.Boolean)value);
      }
      break;

    case HAS_EXCLUDED_ELEMENTS:
      if (value == null) {
        unsetHasExcludedElements();
      } else {
        setHasExcludedElements((java.lang.Boolean)value);
      }
      break;

    case ELEMENTS:
      if (value == null) {
        unsetElements();
      } else {
        setElements((java.util.List<PostDataElement>)value);
      }
      break;

    }
  }

  @org.apache.thrift.annotation.Nullable
  @Override
  public java.lang.Object getFieldValue(_Fields field) {
    switch (field) {
    case IS_READ_ONLY:
      return isIsReadOnly();

    case HAS_EXCLUDED_ELEMENTS:
      return isHasExcludedElements();

    case ELEMENTS:
      return getElements();

    }
    throw new java.lang.IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  @Override
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new java.lang.IllegalArgumentException();
    }

    switch (field) {
    case IS_READ_ONLY:
      return isSetIsReadOnly();
    case HAS_EXCLUDED_ELEMENTS:
      return isSetHasExcludedElements();
    case ELEMENTS:
      return isSetElements();
    }
    throw new java.lang.IllegalStateException();
  }

  @Override
  public boolean equals(java.lang.Object that) {
    if (that instanceof PostData)
      return this.equals((PostData)that);
    return false;
  }

  public boolean equals(PostData that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_isReadOnly = true;
    boolean that_present_isReadOnly = true;
    if (this_present_isReadOnly || that_present_isReadOnly) {
      if (!(this_present_isReadOnly && that_present_isReadOnly))
        return false;
      if (this.isReadOnly != that.isReadOnly)
        return false;
    }

    boolean this_present_hasExcludedElements = true;
    boolean that_present_hasExcludedElements = true;
    if (this_present_hasExcludedElements || that_present_hasExcludedElements) {
      if (!(this_present_hasExcludedElements && that_present_hasExcludedElements))
        return false;
      if (this.hasExcludedElements != that.hasExcludedElements)
        return false;
    }

    boolean this_present_elements = true && this.isSetElements();
    boolean that_present_elements = true && that.isSetElements();
    if (this_present_elements || that_present_elements) {
      if (!(this_present_elements && that_present_elements))
        return false;
      if (!this.elements.equals(that.elements))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + ((isReadOnly) ? 131071 : 524287);

    hashCode = hashCode * 8191 + ((hasExcludedElements) ? 131071 : 524287);

    hashCode = hashCode * 8191 + ((isSetElements()) ? 131071 : 524287);
    if (isSetElements())
      hashCode = hashCode * 8191 + elements.hashCode();

    return hashCode;
  }

  @Override
  public int compareTo(PostData other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = java.lang.Boolean.compare(isSetIsReadOnly(), other.isSetIsReadOnly());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetIsReadOnly()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.isReadOnly, other.isReadOnly);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = java.lang.Boolean.compare(isSetHasExcludedElements(), other.isSetHasExcludedElements());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetHasExcludedElements()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.hasExcludedElements, other.hasExcludedElements);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = java.lang.Boolean.compare(isSetElements(), other.isSetElements());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetElements()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.elements, other.elements);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  @org.apache.thrift.annotation.Nullable
  @Override
  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  @Override
  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    scheme(iprot).read(iprot, this);
  }

  @Override
  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    scheme(oprot).write(oprot, this);
  }

  @Override
  public java.lang.String toString() {
    java.lang.StringBuilder sb = new java.lang.StringBuilder("PostData(");
    boolean first = true;

    sb.append("isReadOnly:");
    sb.append(this.isReadOnly);
    first = false;
    if (!first) sb.append(", ");
    sb.append("hasExcludedElements:");
    sb.append(this.hasExcludedElements);
    first = false;
    if (isSetElements()) {
      if (!first) sb.append(", ");
      sb.append("elements:");
      if (this.elements == null) {
        sb.append("null");
      } else {
        sb.append(this.elements);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // alas, we cannot check 'isReadOnly' because it's a primitive and you chose the non-beans generator.
    // alas, we cannot check 'hasExcludedElements' because it's a primitive and you chose the non-beans generator.
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
    try {
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class PostDataStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    @Override
    public PostDataStandardScheme getScheme() {
      return new PostDataStandardScheme();
    }
  }

  private static class PostDataStandardScheme extends org.apache.thrift.scheme.StandardScheme<PostData> {

    @Override
    public void read(org.apache.thrift.protocol.TProtocol iprot, PostData struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // IS_READ_ONLY
            if (schemeField.type == org.apache.thrift.protocol.TType.BOOL) {
              struct.isReadOnly = iprot.readBool();
              struct.setIsReadOnlyIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // HAS_EXCLUDED_ELEMENTS
            if (schemeField.type == org.apache.thrift.protocol.TType.BOOL) {
              struct.hasExcludedElements = iprot.readBool();
              struct.setHasExcludedElementsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // ELEMENTS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list10 = iprot.readListBegin();
                struct.elements = new java.util.ArrayList<PostDataElement>(_list10.size);
                @org.apache.thrift.annotation.Nullable PostDataElement _elem11;
                for (int _i12 = 0; _i12 < _list10.size; ++_i12)
                {
                  _elem11 = new PostDataElement();
                  _elem11.read(iprot);
                  struct.elements.add(_elem11);
                }
                iprot.readListEnd();
              }
              struct.setElementsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      if (!struct.isSetIsReadOnly()) {
        throw new org.apache.thrift.protocol.TProtocolException("Required field 'isReadOnly' was not found in serialized data! Struct: " + toString());
      }
      if (!struct.isSetHasExcludedElements()) {
        throw new org.apache.thrift.protocol.TProtocolException("Required field 'hasExcludedElements' was not found in serialized data! Struct: " + toString());
      }
      struct.validate();
    }

    @Override
    public void write(org.apache.thrift.protocol.TProtocol oprot, PostData struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      oprot.writeFieldBegin(IS_READ_ONLY_FIELD_DESC);
      oprot.writeBool(struct.isReadOnly);
      oprot.writeFieldEnd();
      oprot.writeFieldBegin(HAS_EXCLUDED_ELEMENTS_FIELD_DESC);
      oprot.writeBool(struct.hasExcludedElements);
      oprot.writeFieldEnd();
      if (struct.elements != null) {
        if (struct.isSetElements()) {
          oprot.writeFieldBegin(ELEMENTS_FIELD_DESC);
          {
            oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.elements.size()));
            for (PostDataElement _iter13 : struct.elements)
            {
              _iter13.write(oprot);
            }
            oprot.writeListEnd();
          }
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class PostDataTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    @Override
    public PostDataTupleScheme getScheme() {
      return new PostDataTupleScheme();
    }
  }

  private static class PostDataTupleScheme extends org.apache.thrift.scheme.TupleScheme<PostData> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, PostData struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      oprot.writeBool(struct.isReadOnly);
      oprot.writeBool(struct.hasExcludedElements);
      java.util.BitSet optionals = new java.util.BitSet();
      if (struct.isSetElements()) {
        optionals.set(0);
      }
      oprot.writeBitSet(optionals, 1);
      if (struct.isSetElements()) {
        {
          oprot.writeI32(struct.elements.size());
          for (PostDataElement _iter14 : struct.elements)
          {
            _iter14.write(oprot);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, PostData struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      struct.isReadOnly = iprot.readBool();
      struct.setIsReadOnlyIsSet(true);
      struct.hasExcludedElements = iprot.readBool();
      struct.setHasExcludedElementsIsSet(true);
      java.util.BitSet incoming = iprot.readBitSet(1);
      if (incoming.get(0)) {
        {
          org.apache.thrift.protocol.TList _list15 = iprot.readListBegin(org.apache.thrift.protocol.TType.STRUCT);
          struct.elements = new java.util.ArrayList<PostDataElement>(_list15.size);
          @org.apache.thrift.annotation.Nullable PostDataElement _elem16;
          for (int _i17 = 0; _i17 < _list15.size; ++_i17)
          {
            _elem16 = new PostDataElement();
            _elem16.read(iprot);
            struct.elements.add(_elem16);
          }
        }
        struct.setElementsIsSet(true);
      }
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}
