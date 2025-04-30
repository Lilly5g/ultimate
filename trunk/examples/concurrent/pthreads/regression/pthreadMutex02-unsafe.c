//#Unsafe
// Author: heizmann@informatik.uni-freiburg.de
// Date: 2018-09-04
//

#include <stdio.h>
#include <pthread.h>

pthread_mutex_t  mutex = PTHREAD_MUTEX_INITIALIZER;

int main()
{
  pthread_mutex_lock(&mutex);
  pthread_mutex_unlock(&mutex);
  int ret = pthread_mutex_unlock(&mutex);
  printf("This line is reachable\n");
  printf("%d", ret);
  //@ assert \false;

  return 0;
}

