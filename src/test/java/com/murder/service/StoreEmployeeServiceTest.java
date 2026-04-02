package com.murder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.result.PageResult;
import com.murder.dto.StoreEmployeeDTO;
import com.murder.entity.Store;
import com.murder.entity.StoreEmployee;
import com.murder.mapper.StoreEmployeeMapper;
import com.murder.mapper.StoreMapper;
import com.murder.service.impl.StoreEmployeeServiceImpl;
import com.murder.vo.StoreEmployeeVO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 门店员工服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("门店员工服务测试")
class StoreEmployeeServiceTest {

    @Mock
    private StoreEmployeeMapper employeeMapper;

    @Mock
    private StoreMapper storeMapper;

    @InjectMocks
    private StoreEmployeeServiceImpl storeEmployeeService;

    private StoreEmployee testEmployee;
    private Store testStore;
    private StoreEmployeeDTO testDTO;

    @BeforeEach
    void setUp() {
        testStore = Store.builder()
                .id(1L)
                .name("测试门店")
                .build();

        testEmployee = new StoreEmployee();
        testEmployee.setId(1L);
        testEmployee.setStoreId(1L);
        testEmployee.setName("张员工");
        testEmployee.setPhone("13800138000");
        testEmployee.setPosition(1); // 店长
        testEmployee.setStatus(1);
        testEmployee.setCreateTime(LocalDateTime.now());

        testDTO = new StoreEmployeeDTO();
        testDTO.setId(1L);
        testDTO.setStoreId(1L);
        testDTO.setName("张员工");
        testDTO.setPhone("13800138000");
        testDTO.setPosition(1);
    }

    @Nested
    @DisplayName("分页查询测试")
    class PageQueryTests {

        @Test
        @DisplayName("分页查询员工 - 按门店过滤")
        void testPageQuery_ByStoreId() {
            List<StoreEmployee> employees = Collections.singletonList(testEmployee);
            when(employeeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            when(employeeMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        Page<StoreEmployee> p = inv.getArgument(0);
                        p.setRecords(employees);
                        return p;
                    });
            when(storeMapper.selectBatchIds(anyList())).thenReturn(Collections.singletonList(testStore));

            PageResult<StoreEmployeeVO> result = storeEmployeeService.pageQuery(1L, 1, 10);

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
            assertFalse(result.getRecords().isEmpty());
            assertEquals("张员工", result.getRecords().get(0).getName());
        }

        @Test
        @DisplayName("分页查询员工 - 无过滤条件")
        void testPageQuery_NoFilter() {
            when(employeeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(employeeMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        Page<StoreEmployee> p = inv.getArgument(0);
                        p.setRecords(Collections.emptyList());
                        return p;
                    });

            PageResult<StoreEmployeeVO> result = storeEmployeeService.pageQuery(null, 1, 10);

            assertNotNull(result);
            assertEquals(0L, result.getTotal());
        }
    }

    @Nested
    @DisplayName("查询测试")
    class QueryTests {

        @Test
        @DisplayName("按门店ID查询员工列表")
        void testListByStoreId() {
            when(employeeMapper.selectList(any())).thenReturn(Arrays.asList(testEmployee));

            List<StoreEmployee> result = storeEmployeeService.listByStoreId(1L);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("张员工", result.get(0).getName());
        }

        @Test
        @DisplayName("按ID查询员工 - 存在")
        void testGetById_Found() {
            when(employeeMapper.selectById(1L)).thenReturn(testEmployee);

            StoreEmployee result = storeEmployeeService.getById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("按ID查询员工 - 不存在")
        void testGetById_NotFound() {
            when(employeeMapper.selectById(999L)).thenReturn(null);

            StoreEmployee result = storeEmployeeService.getById(999L);

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("新增/更新/删除测试")
    class CrudTests {

        @Test
        @DisplayName("新增员工 - 默认状态为在职")
        void testAdd_DefaultStatusActive() {
            when(employeeMapper.insert(any(StoreEmployee.class))).thenReturn(1);
            ArgumentCaptor<StoreEmployee> employeeCaptor = ArgumentCaptor.forClass(StoreEmployee.class);

            storeEmployeeService.add(testDTO);
            verify(employeeMapper, times(1)).insert(employeeCaptor.capture());

            StoreEmployee employee = employeeCaptor.getValue();
            assertEquals(1L, employee.getStoreId());
            assertEquals("张员工", employee.getName());
            assertEquals("13800138000", employee.getPhone());
            assertEquals(1, employee.getPosition());
            assertEquals(1, employee.getStatus(), "新增员工默认状态应为1(在职)");
        }

        @Test
        @DisplayName("更新员工 - 成功")
        void testUpdate_Success() {
            // update() 内部先 selectById 检查员工是否存在，需要 mock
            when(employeeMapper.selectById(1L)).thenReturn(testEmployee);
            when(employeeMapper.selectOne(any())).thenReturn(null); // validateLoginAccountUnique 无重复
            when(employeeMapper.updateById(any(StoreEmployee.class))).thenReturn(1);
            ArgumentCaptor<StoreEmployee> employeeCaptor = ArgumentCaptor.forClass(StoreEmployee.class);

            testDTO.setName("更新后的员工名");
            storeEmployeeService.update(testDTO);
            verify(employeeMapper, times(1)).updateById(employeeCaptor.capture());

            StoreEmployee updated = employeeCaptor.getValue();
            assertEquals(1L, updated.getId());
            assertEquals(1L, updated.getStoreId());
            assertEquals("更新后的员工名", updated.getName());
            assertEquals("13800138000", updated.getPhone());
            assertEquals(1, updated.getPosition());
        }

        @Test
        @DisplayName("删除员工 - 成功")
        void testDelete_Success() {
            when(employeeMapper.deleteById(1L)).thenReturn(1);

            storeEmployeeService.delete(1L);
            verify(employeeMapper, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("批量删除员工 - 成功")
        void testBatchDelete_Success() {
            when(employeeMapper.deleteBatchIds(anyList())).thenReturn(2);

            List<Long> ids = Arrays.asList(1L, 2L);
            storeEmployeeService.batchDelete(ids);
            verify(employeeMapper, times(1)).deleteBatchIds(ids);
        }

        @Test
        @DisplayName("更新员工状态 - 离职")
        void testUpdateStatus_Resigned() {
            when(employeeMapper.updateById(any(StoreEmployee.class))).thenReturn(1);
            ArgumentCaptor<StoreEmployee> employeeCaptor = ArgumentCaptor.forClass(StoreEmployee.class);

            storeEmployeeService.updateStatus(1L, 0);
            verify(employeeMapper, times(1)).updateById(employeeCaptor.capture());

            StoreEmployee updated = employeeCaptor.getValue();
            assertEquals(1L, updated.getId());
            assertEquals(0, updated.getStatus());
        }
    }

    @Nested
    @DisplayName("职位名称转换测试")
    class PositionNameTests {

        @Test
        @DisplayName("职位名称正确转换")
        void testPositionName_Correct() {
            List<StoreEmployee> employees = Collections.singletonList(testEmployee);
            when(employeeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            when(employeeMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        Page<StoreEmployee> p = inv.getArgument(0);
                        p.setRecords(employees);
                        return p;
                    });
            when(storeMapper.selectBatchIds(anyList())).thenReturn(Collections.singletonList(testStore));

            PageResult<StoreEmployeeVO> result = storeEmployeeService.pageQuery(1L, 1, 10);

            assertNotNull(result);
            assertFalse(result.getRecords().isEmpty());
            assertEquals("店长", result.getRecords().get(0).getPositionName());
        }
    }
}
