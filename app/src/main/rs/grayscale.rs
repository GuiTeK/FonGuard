#pragma version(1)
#pragma rs java_package_name(com.fonguard)
#pragma rs_fp_relaxed

uchar4 RS_KERNEL toGrayscale(uchar4 in)
{
    float4 inF = rsUnpackColor8888(in);
    float grayscale = 0.299 * inF.r + 0.587 * inF.g + 0.114 * inF.b;

    return rsPackColorTo8888(grayscale, grayscale, grayscale, inF.a);
}
