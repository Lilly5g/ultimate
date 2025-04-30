//#Safe
// Author: heizmann@informatik.uni-freiburg.de
// Date: 2018-09-04
//
// Safe because second lock blocks execution.

#include <stdio.h>
#include <pthread.h>

pthread_mutex_t  mutex;

int main()
{
  pthread_mutex_init(&mutex, 0);
  int ret = pthread_mutex_lock(&mutex);
  if (ret != 0) {
    //@ assert \false;
  }
  pthread_mutex_lock(&mutex);
  printf("This line is reachable\n");
  //@ assert \false;

  return 0;
}

