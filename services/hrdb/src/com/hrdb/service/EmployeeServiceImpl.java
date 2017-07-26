/*Copyright (c) 2015-2016 imaginea-com All Rights Reserved.
 This software is the confidential and proprietary information of imaginea-com You shall not disclose such Confidential Information and shall use it only in accordance
 with the terms of the source code license agreement you entered into with imaginea-com*/
package com.hrdb.service;

/*This is a Studio Managed File. DO NOT EDIT THIS FILE. Your changes may be reverted by Studio.*/

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wavemaker.runtime.data.dao.WMGenericDao;
import com.wavemaker.runtime.data.exception.EntityNotFoundException;
import com.wavemaker.runtime.data.export.ExportType;
import com.wavemaker.runtime.data.expression.QueryFilter;
import com.wavemaker.runtime.data.model.AggregationInfo;
import com.wavemaker.runtime.file.model.Downloadable;

import com.hrdb.Employee;
import com.hrdb.Vacation;


/**
 * ServiceImpl object for domain model class Employee.
 *
 * @see Employee
 */
@Service("hrdb.EmployeeService")
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    @Autowired
	@Qualifier("hrdb.VacationService")
	private VacationService vacationService;

    @Autowired
    @Qualifier("hrdb.EmployeeDao")
    private WMGenericDao<Employee, Integer> wmGenericDao;

    public void setWMGenericDao(WMGenericDao<Employee, Integer> wmGenericDao) {
        this.wmGenericDao = wmGenericDao;
    }

    @Transactional(value = "hrdbTransactionManager")
    @Override
	public Employee create(Employee employee) {
        LOGGER.debug("Creating a new Employee with information: {}", employee);
        Employee employeeCreated = this.wmGenericDao.create(employee);
        if(employeeCreated.getEmployeesForManagerId() != null) {
            for(Employee employeesForManagerId : employeeCreated.getEmployeesForManagerId()) {
                employeesForManagerId.setEmployeeByManagerId(employeeCreated);
                LOGGER.debug("Creating a new child Employee with information: {}", employeesForManagerId);
                create(employeesForManagerId);
            }
        }

        if(employeeCreated.getVacations() != null) {
            for(Vacation vacation : employeeCreated.getVacations()) {
                vacation.setEmployee(employeeCreated);
                LOGGER.debug("Creating a new child Vacation with information: {}", vacation);
                vacationService.create(vacation);
            }
        }
        return employeeCreated;
    }

	@Transactional(readOnly = true, value = "hrdbTransactionManager")
	@Override
	public Employee getById(Integer employeeId) throws EntityNotFoundException {
        LOGGER.debug("Finding Employee by id: {}", employeeId);
        Employee employee = this.wmGenericDao.findById(employeeId);
        if (employee == null){
            LOGGER.debug("No Employee found with id: {}", employeeId);
            throw new EntityNotFoundException(String.valueOf(employeeId));
        }
        return employee;
    }

    @Transactional(readOnly = true, value = "hrdbTransactionManager")
	@Override
	public Employee findById(Integer employeeId) {
        LOGGER.debug("Finding Employee by id: {}", employeeId);
        return this.wmGenericDao.findById(employeeId);
    }


	@Transactional(rollbackFor = EntityNotFoundException.class, value = "hrdbTransactionManager")
	@Override
	public Employee update(Employee employee) throws EntityNotFoundException {
        LOGGER.debug("Updating Employee with information: {}", employee);
        this.wmGenericDao.update(employee);

        Integer employeeId = employee.getEmpId();

        return this.wmGenericDao.findById(employeeId);
    }

    @Transactional(value = "hrdbTransactionManager")
	@Override
	public Employee delete(Integer employeeId) throws EntityNotFoundException {
        LOGGER.debug("Deleting Employee with id: {}", employeeId);
        Employee deleted = this.wmGenericDao.findById(employeeId);
        if (deleted == null) {
            LOGGER.debug("No Employee found with id: {}", employeeId);
            throw new EntityNotFoundException(String.valueOf(employeeId));
        }
        this.wmGenericDao.delete(deleted);
        return deleted;
    }

	@Transactional(readOnly = true, value = "hrdbTransactionManager")
	@Override
	public Page<Employee> findAll(QueryFilter[] queryFilters, Pageable pageable) {
        LOGGER.debug("Finding all Employees");
        return this.wmGenericDao.search(queryFilters, pageable);
    }

    @Transactional(readOnly = true, value = "hrdbTransactionManager")
    @Override
    public Page<Employee> findAll(String query, Pageable pageable) {
        LOGGER.debug("Finding all Employees");
        return this.wmGenericDao.searchByQuery(query, pageable);
    }

    @Transactional(readOnly = true, value = "hrdbTransactionManager")
    @Override
    public Downloadable export(ExportType exportType, String query, Pageable pageable) {
        LOGGER.debug("exporting data in the service hrdb for table Employee to {} format", exportType);
        return this.wmGenericDao.export(exportType, query, pageable);
    }

	@Transactional(readOnly = true, value = "hrdbTransactionManager")
	@Override
	public long count(String query) {
        return this.wmGenericDao.count(query);
    }

    @Transactional(readOnly = true, value = "hrdbTransactionManager")
	@Override
    public Page<Map<String, Object>> getAggregatedValues(AggregationInfo aggregationInfo, Pageable pageable) {
        return this.wmGenericDao.getAggregatedValues(aggregationInfo, pageable);
    }

    @Transactional(readOnly = true, value = "hrdbTransactionManager")
    @Override
    public Page<Employee> findAssociatedEmployeesForManagerId(Integer empId, Pageable pageable) {
        LOGGER.debug("Fetching all associated employeesForManagerId");

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("employeeByManagerId.empId = '" + empId + "'");

        return findAll(queryBuilder.toString(), pageable);
    }

    @Transactional(readOnly = true, value = "hrdbTransactionManager")
    @Override
    public Page<Vacation> findAssociatedVacations(Integer empId, Pageable pageable) {
        LOGGER.debug("Fetching all associated vacations");

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("employee.empId = '" + empId + "'");

        return vacationService.findAll(queryBuilder.toString(), pageable);
    }

    /**
	 * This setter method should only be used by unit tests
	 *
	 * @param service VacationService instance
	 */
	protected void setVacationService(VacationService service) {
        this.vacationService = service;
    }

}

