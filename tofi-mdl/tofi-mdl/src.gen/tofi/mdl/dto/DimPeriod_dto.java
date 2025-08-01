/* THIS FILE GENERATED! NOT MODIFY! */
package tofi.mdl.dto;

import jandcode.commons.datetime.*;
import jandcode.commons.reflect.*;

/**
 * Измерение периодов
* <p>
    * table: DimPeriod
    */
    public class DimPeriod_dto {

    private Long id;

    @FieldProps(dict = "FD_AccessLevel")
    private Long accessLevel;

    @FieldProps(dict = "FD_PeriodNameTml")
    private Long periodNameTml;

    @FieldProps(size = 30)
    private String cod;

    @FieldProps(size = 200)
    private String name;

    @FieldProps(size = 400)
    private String fullName;

    private XDate dbeg;

    private XDate dend;

    private String cmt;

    /**
    * id
    */
    public Long getId() {
    return this.id;
    }

    public void setId(Long v) {
    this.id = v;
    }

    /**
    * Уровень доступа
    */
    public Long getAccessLevel() {
    return this.accessLevel;
    }

    public void setAccessLevel(Long v) {
    this.accessLevel = v;
    }

    /**
    * Шаблон наименования периодов
    */
    public Long getPeriodNameTml() {
    return this.periodNameTml;
    }

    public void setPeriodNameTml(Long v) {
    this.periodNameTml = v;
    }

    /**
    * Код
    */
    public String getCod() {
    return this.cod;
    }

    public void setCod(String v) {
    this.cod = v;
    }

    /**
    * Краткое наименование
    */
    public String getName() {
    return this.name;
    }

    public void setName(String v) {
    this.name = v;
    }

    /**
    * Полное наименование
    */
    public String getFullName() {
    return this.fullName;
    }

    public void setFullName(String v) {
    this.fullName = v;
    }

    /**
    * Начало интервала
    */
    public XDate getDbeg() {
    return this.dbeg;
    }

    public void setDbeg(XDate v) {
    this.dbeg = v;
    }

    /**
    * Конец интервала
    */
    public XDate getDend() {
    return this.dend;
    }

    public void setDend(XDate v) {
    this.dend = v;
    }

    /**
    * Комментарий
    */
    public String getCmt() {
    return this.cmt;
    }

    public void setCmt(String v) {
    this.cmt = v;
    }

    }
