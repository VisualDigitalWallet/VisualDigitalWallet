#include <iostream>
#include <string>
using namespace std;
// 输入androidID 8位string(carrier或者本机读取)，
// 总共多少个分存图n,第几个分存图splitID(能从carrier获得)
// 分存用的矩阵s0,s1(int**)(写死的数据传入)
// 需要检查的矩阵toDetect(int**)
bool detect(string androidID,int n, int splitID, int** s0, int** s1, int** toDetect)
{
	bool isCheater = false;
	//生成序列
	int queue[8];
	for (int i = 0; i < 8; i++)
	{
		queue[i] = (int(androidID[i]) % n);
	}
	//第n(0开始)张分存图检查的位置在第n*8~n*8+7位
	int start = splitID * 8;
	int end = start + 7;
	//计算膨胀度
	int m = sizeof(s0[0]) / sizeof(s0[0][0]);
	int edge = sqrt(m);
	//生成用于比较的矩阵comp0,comp1
	/*
	* 以comp0为例，comp0[8][edge][edge]
	* 第一个index对应n*8~n*8+7
	* 第二个index对应膨胀后矩形纵坐标，第三个是横坐标
	*/
	int*** comp0 = new int** [8];
	int*** comp1 = new int** [8];
	for (int i = 0; i < 8; i++)
	{
		comp0[i] = new int* [edge];
		comp1[i] = new int* [edge];
		for (int j = 0; j < edge; j++) 
		{
			comp0[i][j] = new int[edge];
			comp1[i][j] = new int[edge];
		}
	}
	for (int x = 0; x < 8; x++)
	{
		for (int y = 0; y < edge; y++)
		{
			for (int z = 0; z < edge; z++)
			{
				comp0[x][y][z] = s0[queue[x]][y * edge + z];
				comp1[x][y][z] = s1[queue[x]][y * edge + z];
			}
		}
	}
	//找到对应位置并比较
	int startX = start / 16;
	int startY = start % 16;
	for (int i = 0; i < 8; i++)
	{
		for (int x = 0; x < edge; x++)
		{
			for (int y = 0; y < edge; y++)
			{
				if (toDetect[startX * edge + x][startY * edge + y] != comp0[i][x][y]) 
				{
					if (toDetect[startX * edge + x][startY * edge + y] != comp1[i][x][y])
					{
						isCheater = true;
					}
				}
			}
		}
	}
	
	return isCheater;
}
