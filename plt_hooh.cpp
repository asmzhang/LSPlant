#include <stdio.h>
#include <dlfcn.h> 
#include <link.h>
#include <string.h>
#include <memory.h>
#include <stdlib.h>
#include <linux/elf.h>
#include <elf.h>
#include <sys/mman.h>

int myprintf(const char* _fmt, int n)
{
    printf("hook come on: _fmt:%s n:%n \n", _fmt, n);
    return printf(_fmt, n);
}

int DipCallback(struct dl_phdr_info* pDPI, size_t, void*_Nullable pData)
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

    //解析动态段
    while (pDyns->d_tag != DT_NULL)
    {
        switch (pDyns->d_tag)
        {
            case DT_STRTAB:
                pStrTable = (char*)dpi.dlpi_addr+pDyns->d_un.d_ptr;
                break;
            case DT_SYMTAB:
                pSymTable = (Elf64_Sym*)(dpi.dlpi_addr+pDyns->d_un.d_ptr);
                break;
            case DT_RELA:
                pRelaDyn = (Elf64_Rela*)(dpi.dlpi_addr+pDyns->d_un.d_ptr);
                break;
            case DT_RELASZ:
                nNumOfRela = pDyns->d_un.d_val / sizeof(Elf64_Rela);
                break;
            case DT_JMPREL:
                pRelaPlt = (Elf64_Rela*)(dpi.dlpi_addr+pDyns->d_un.d_ptr);
                break;
            case DT_PLTRELSZ:
                nNumOfRelaPlt = pDyns->d_un.d_val / sizeof(Elf64_Rela);
                break;
            default:
                break;
        }
        pDyns++;
    }


    //3.遍历重定位表,定位printf的地址
    uint64_t* pfnPrinft = NULL;
    for (size_t i = 0; i < nNumOfRelaPlt; i++)
    {
        uint32_t nSym = ELF64_R_SYM(pRelaPlt[i].r_info); 
        uint32_t nType = ELF64_R_TYPE(pRelaPlt[i].r_info);
        //根据符号获取地址


        //printf("fun:%s \r\n",pStrTable+pSymTable[nSym].st_value);
        if (strcmp(pStrTable+pSymTable[nSym].st_name, "printf") == 0)
        {
            pfnPrinft = (uint64_t*)(pRelaPlt[i].r_offset + dpi.dlpi_addr); 
            break;
        }

    }

    //4. 替换
    printf("开始替换...");
    void* pPageBase=(void*)((uint64_t)pfnPrinft & (~0xfffl)); 
    mprotect(pPageBase, 1, PROT_WRITE | PROT_READ|PROT_EXEC);

    *pfnPrinft = (uint64_t)myprintf;
}
