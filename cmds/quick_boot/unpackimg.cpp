#include "unpackimg.h"
#include <stdlib.h>
#include <stdio.h>

#define IH_MAGIC        0x27051956      /* Image Magic Number           */
#define IH_NMLEN                32      /* Image Name Length            */

#define AML_RES_IMG_VERSION         0x01
#define AML_RES_IMG_V1_MAGIC_LEN    8
#define AML_RES_IMG_V1_MAGIC        "AML_RES!"//8 chars
#define AML_RES_IMG_ITEM_ALIGN_SZ   16
#define AML_RES_IMG_HEAD_SZ         (AML_RES_IMG_ITEM_ALIGN_SZ * 4)//64
#define AML_RES_ITEM_HEAD_SZ        (AML_RES_IMG_ITEM_ALIGN_SZ * 4)//64

#pragma pack(push, 1)
typedef struct pack_header{
        unsigned int    magic;  /* Image Header Magic Number    */
        unsigned int    hcrc;   /* Image Header CRC Checksum    */
        unsigned int    size;   /* Image Data Size              */
        unsigned int    start;  /* item data offset in the image*/
        unsigned int    end;            /* Entry Point Address          */
        unsigned int    next;   /* Next item head offset in the image*/
        unsigned int    dcrc;   /* Image Data CRC Checksum      */
        unsigned char   index;          /* Operating System             */
        unsigned char   nums;   /* CPU architecture             */
        unsigned char   type;   /* Image Type                   */
        unsigned char   comp;   /* Compression Type             */
        char    name[IH_NMLEN]; /* Image Name           */
}AmlResItemHead_t;
#pragma pack(pop)


//typedef for amlogic resource image
#pragma pack(push, 4)
typedef struct {
    __u32   crc;    //crc32 value for the resouces image
    __s32   version;//current version is 0x01

    __u8    magic[AML_RES_IMG_V1_MAGIC_LEN];  //resources images magic

    __u32   imgSz;  //total image size in byte
    __u32   imgItemNum;//total item packed in the image

    __u32   alignSz;//AML_RES_IMG_ITEM_ALIGN_SZ
    __u8    reserv[AML_RES_IMG_HEAD_SZ - 8 * 3 - 4];

}AmlResImgHead_t;
#pragma pack(pop)

#define LOGO_PARTATION_NODE "/dev/block/logo"

int read_logo(const char* logo_name, char** ppBuf) {
	int buf_len = 0;
	FILE* pFile = NULL;
	size_t result;

	pFile = fopen(LOGO_PARTATION_NODE , "rb");
	if( pFile == NULL ) {
		printf("read_logo open %s fail!\n", LOGO_PARTATION_NODE);
		return 0;
	}

	fseek(pFile , 0 , SEEK_SET);
	AmlResImgHead_t imghead;
	result = fread(&imghead, 1, sizeof(imghead), pFile);
	if( result != sizeof(imghead) ) {
		printf("read_logo read AmlResImgHead_t from %s fail!\n", LOGO_PARTATION_NODE);
		return 0;
	}

	//printf("imgSz:%d imgItemNum:%d\n", imghead.imgSz, imghead.imgItemNum);

	fseek(pFile, sizeof(AmlResImgHead_t), SEEK_SET);
	AmlResItemHead_t itemhead;
	while( fread(&itemhead, 1, sizeof(itemhead), pFile) == sizeof(itemhead) ) {
		if(itemhead.magic != IH_MAGIC) {
			break;
		}
		//printf("itemhead.name:%s logo_name:%s\n", itemhead.name, logo_name);	
		
		if(!memcmp(logo_name, itemhead.name, strlen(logo_name))) {
			*ppBuf = (char*)malloc(itemhead.size);
			fseek(pFile, itemhead.start, SEEK_SET);
			fread(*ppBuf, itemhead.size, 1, pFile);
			buf_len = itemhead.size;
			break;
		}
	
		fseek(pFile, itemhead.next, SEEK_SET);
	}

	if(pFile != NULL) {
		fclose(pFile);
	}
	
	return buf_len;
}
