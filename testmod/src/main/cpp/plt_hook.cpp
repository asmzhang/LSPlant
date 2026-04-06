#include <stdio.h>
#include <dlfcn.h> 
#include <link.h>
#include <string.h>
#include <memory.h>
#include <unistd.h>
#include <stdlib.h>
#include <linux/elf.h>
#include <elf.h>
#include <sys/mman.h>
#include <stdint.h>

int myprintf(const char* _fmt, int n)
{
    // [修复] 原先的 %n 会导致把当前打印出的字符数量强制写入到 n 这个地址指向的内存从而引发越界崩溃(segfault)
    // 根据打印内容的意图，应该将格式化符号替换为 %d 打印数字
    printf("hook come on: _fmt:%s n:%d \n", _fmt, n);
    return printf(_fmt, n);
}

// [修复] 使用标准的 void* 而非 _Nullable 编译器特定扩展，提高兼容性
int DipCallback(struct dl_phdr_info* pDPI, size_t, void* pData)
{
    if (strstr(pDPI->dlpi_name, "main") != NULL)
    {
        memcpy(pData, pDPI, sizeof(struct dl_phdr_info)); 
        return -1;
    }
    
    return 0; //继续遍历
}

__attribute__((constructor)) void hookPlt() 
{
    //1. 获取main的模块基址
    struct dl_phdr_info dpi = {0};
    dl_iterate_phdr(DipCallback, &dpi);

    //2.解析文件格式,获取重定位表
    char* pStrTable = NULL; 
    size_t nSizeOfStrtable = 0;
    Elf64_Sym* pSymTable = NULL;
    Elf64_Rela* pRelaDyn = NULL;
    size_t nNumOfRela = 0;
    Elf64_Rela* pRelaPlt = NULL;
    size_t nNumOfRelaPlt = 0;

    Elf64_Dyn* pDyns = NULL;
    size_t nNumOfDyns = 0;
    for (size_t i = 0; i < dpi.dlpi_phnum; i++)
    {
        if (dpi.dlpi_phdr[i].p_type == PT_DYNAMIC)
        {
            pDyns = (Elf64_Dyn*)(dpi.dlpi_addr+dpi.dlpi_phdr[i].p_vaddr);
            break;
        }
    }

    if (!pDyns) return;

    //解析动态段
    while (pDyns->d_tag != DT_NULL)
    {
        switch (pDyns->d_tag)
        {
            case DT_STRTAB:
                // [优化] 明确将指针基址与偏移先行相加，再做转换，避免运算符优先级视觉干扰
                pStrTable = (char*)(dpi.dlpi_addr + pDyns->d_un.d_ptr);
                break;
            case DT_SYMTAB:
                pSymTable = (Elf64_Sym*)(dpi.dlpi_addr + pDyns->d_un.d_ptr);
                break;
            case DT_RELA:
                pRelaDyn = (Elf64_Rela*)(dpi.dlpi_addr + pDyns->d_un.d_ptr);
                break;
            case DT_RELASZ:
                nNumOfRela = pDyns->d_un.d_val / sizeof(Elf64_Rela);
                break;
            case DT_JMPREL:
                pRelaPlt = (Elf64_Rela*)(dpi.dlpi_addr + pDyns->d_un.d_ptr);
                break;
            case DT_PLTRELSZ:
                nNumOfRelaPlt = pDyns->d_un.d_val / sizeof(Elf64_Rela);
                break;
            default:
                break;
        }
        pDyns++;
    }

    if (!pStrTable || !pSymTable || !pRelaPlt) return;

    //3.遍历重定位表,定位printf的地址
    uint64_t* pfnPrinft = NULL;
    for (size_t i = 0; i < nNumOfRelaPlt; i++)
    {
        uint32_t nSym = ELF64_R_SYM(pRelaPlt[i].r_info); 
        uint32_t nType = ELF64_R_TYPE(pRelaPlt[i].r_info);

        if (strcmp(pStrTable+pSymTable[nSym].st_name, "printf") == 0)
        {
            pfnPrinft = (uint64_t*)(pRelaPlt[i].r_offset + dpi.dlpi_addr); 
            break;
        }
    }

    if (!pfnPrinft) return;

    //4. 替换
    printf("开始替换...");
    
    // [修复] 不应该硬编码页对齐的掩码(~0xfffl)，在最新的安卓与高通芯片上常常默认页大小是 16KB！
    // 采用底层的 sysconf(_SC_PAGE_SIZE) 来动态确定对齐掩码，并且在 mprotect 时确保扩展两页的大小避免因边界跨页而奔溃。
    long page_size = sysconf(_SC_PAGE_SIZE);
    void* pPageBase = (void*)((uintptr_t)pfnPrinft & ~(page_size - 1)); 
    mprotect(pPageBase, page_size * 2, PROT_WRITE | PROT_READ | PROT_EXEC);

    *pfnPrinft = (uint64_t)myprintf;
}
